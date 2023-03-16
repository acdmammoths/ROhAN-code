import json
import math
import os
import os.path
import subprocess
import sys

maxJobs = 1000  # number of jobs that can be submitted at the same time
email = "mabuissa24@amherst.edu"  # address for cluster emails


def checkOrMakeDir(path):
    """Helper method to check that a directory exists, and otherwise create it"""
    path = os.path.abspath(path)
    if not os.path.exists(path):
        os.makedirs(path)


def checkConfig(config):
    for key in ("resultsDir", "logsDir", "numEstSamples", "numWySamples",
                "datasetPath", "samplers", "numThreads"):
        if key not in config:
            sys.exit(f"Error: conf key '{key}' needed but not found.")


def main():  # TODO: Write a script to cleanup and run python script
    if len(sys.argv) != 4:
        sys.exit(f"Usage: {sys.argv[0]} confFile partition scriptsDir")
    # Open and read config file from command-line
    try:
        with open(sys.argv[1]) as configPath:
            try:
                config = json.load(configPath)
            except json.JSONDecodeError as e:
                sys.exit(f"Error decoding JSON file '{sys.argv[1]}': {e}")
    except OSError as e:
        sys.exit(f"Error reading conf file '{sys.argv[1]}': {e}")
    checkConfig(config)
    # Read configuration values, checking that directories exist
    resultsDir = os.path.abspath(config['resultsDir'])
    checkOrMakeDir(resultsDir)
    logsDir = os.path.abspath(config['logsDir'])
    checkOrMakeDir(logsDir)
    numSamples = {
        "est": config['numEstSamples'],
        "wy": config['numWySamples']
    }
    sampleTypes = ['est', 'wy']
    dataname = os.path.basename(config['datasetPath']).split('.')[0]
    processOnly = config["processOnly"]
    wyOnly = config["wyOnly"]
    if wyOnly:
        sampleTypes = ['wy']

    # Read partition from command-line and make sure it's valid
    partition = sys.argv[2].lower()
    if partition not in {'cpu', 'cpu-long', 'cpu-preempt'}:
        sys.exit(f"Error: '{partition}' is not a valid partition.")

    # Read scriptsDir from command-line, make sure it exists
    scriptsDir = sys.argv[3]
    checkOrMakeDir(scriptsDir)

    # Begin creation of #SBATCH files
    for sampler in config['samplers']:
        jobIDs = []  # needed for the dependencies of the processing job
        outerPath = os.path.join(resultsDir, sampler)
        checkOrMakeDir(outerPath)
        batchSize = sum(numSamples.values()) * len(config['samplers']) // maxJobs + 1
        for sampleType in sampleTypes:
            if not processOnly:
                checkOrMakeDir(os.path.join(outerPath, "samples"))
                checkOrMakeDir(os.path.join(outerPath, "freqItemsets"))

                # Create the sbatch script for this combination of sampler and sampleType
                arraySize = math.ceil(numSamples[sampleType] / batchSize)
                sampleScript = os.path.join(scriptsDir,
                                            f"sample_{dataname}_{sampler}_{sampleType}.sh")
                try:
                    with open(sampleScript, 'w') as outfile:
                        outfile.write(f"""#!/bin/bash
#SBATCH --mail-user={email}
#SBATCH --mail-type=FAIL
#SBATCH -J {dataname}_{sampler}_{sampleType}_sample
#SBATCH -p {partition}
# Currently, the job array's last task may create extra samples that will be
# ignored when processing the results
#SBATCH --array=0-{arraySize - 1}
#SBATCH -o {logsDir}/{dataname}_{sampler}_{sampleType}_%A_%a.out
#SBATCH -e {logsDir}/{dataname}_{sampler}_{sampleType}_%A_%a.err

module load java

BATCHSIZE={batchSize}

for i in $(seq 1 $BATCHSIZE); do
    REALID=$(($BATCHSIZE * $SLURM_ARRAY_TASK_ID + $i - 1))
    java -cp target/ROhAN-1.0-SNAPSHOT-jar-with-dependencies.jar \
            rohan.mining.SampleAndMineTask \
            {os.path.abspath(sys.argv[1])} \
            {resultsDir}/{sampler}/samples/{dataname}-{sampleType}-$REALID.txt \
            {resultsDir}/{sampler}/freqItemsets/{dataname}-{sampleType}-$REALID.txt \
                    {sampler}
done""")
                except OSError as e:
                    sys.exit(f"Error writing file '{sampleScript}': {e}")

                try:
                    jobID = subprocess.run(f"sbatch {sampleScript}", shell=True,
                                           capture_output=True, check=True, text=True).stdout
                except subprocess.CalledProcessError as e:
                    sys.exit(f"Error running sbatch script '{sampleScript}': {e}")
                jobID = jobID.split()[-1]
                jobIDs.append(jobID)

        # Create the processing sbatch script for this sampler
        sigFreqItemsetPath = os.path.join(resultsDir, sampler,
                                          "sigFreqItemsets")
        checkOrMakeDir(sigFreqItemsetPath)
        processingFile = os.path.join(scriptsDir, f"processing_{dataname}_{sampler}.sh")
        try:
            with open(processingFile, 'w') as outfile:
                outfile.write(f"""#!/bin/bash
#SBATCH --mail-user={email}
#SBATCH --mail-type=END
#SBATCH --mail-type=FAIL
#SBATCH -J {dataname}_{sampler}_processing
# The processing code uses threads for parallelism, so we specify the number of
# cpus to use
#SBATCH -c {config['numThreads']}
#SBATCH -p {partition}
#SBATCH -o {logsDir}/{dataname}_{sampler}_processing.out
#SBATCH -e {logsDir}/{dataname}_{sampler}_processing.err""")
                # Only add dependencies if processOnly flag is false
                if not processOnly:
                    outfile.write(f"""
# Add dependencies on the sample job arrays
#SBATCH --dependency=afterok:{",".join(jobIDs)}""")
                outfile.write(f"""
module load java
java -cp target/ROhAN-1.0-SNAPSHOT-jar-with-dependencies.jar \
                rohan.mining.SigFreqItemsetProcessing \
                {os.path.abspath(sys.argv[1])} {sampler}""")
        except OSError as e:
            sys.exit(f"Error writing file '{processingFile}': {e}")

        try:
            # Capture the output even if we do not do anything with it
            subprocess.run(f"sbatch {processingFile}", shell=True, check=True, capture_output=True)
        except subprocess.CalledProcessError as e:
            sys.exit(f"Error running sbatch script '{processingFile}': {e}")


if __name__ == '__main__':
    main()
