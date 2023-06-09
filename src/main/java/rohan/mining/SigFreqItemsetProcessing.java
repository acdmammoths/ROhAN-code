package rohan.mining;

/*
 * The ROhAN algorithm for mining significant frequent itemsets.
 *
 * Copyright (C) 2022 Alexander Lee, Maryam Abuissa, and Matteo Riondato
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
import rohan.io.JsonFile;
import rohan.io.JsonKeys;
import rohan.io.Paths;
import rohan.samplers.Sampler;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

/** A class to process significant frequent itemsets using the Westfall-Young method. */
public class SigFreqItemsetProcessing {
    /** The sampler to obtain samples. */
    private final Sampler sampler;

    /** The number of swaps to run in the chain. */
    private final int numSwaps;

    /** The number of samples to obtain for p-value estimation. */
    private final int numEstSamples;

    /** The number of samples to obtain to compute the adjusted critical value. */
    private final int numWySamples;

    /** The minimum frequency to mine frequent itemsets. */
    private final double minFreq;

    /** The family wise error rate. */
    private final double fwer;

    /** The number of threads to use. */
    private final int numThreads;

    /** The random seed to use for replication. */
    private final long seed;

    /** An object to get necessary paths. */
    private final Paths paths;

    /** Whether to cleanup the sampled matrices and frequent itemsets. */
    private final boolean cleanup;

    /** An array of minimum p-values. */
    private double[] minPvalues;

    /** The adjusted critical value. */
    private double adjustedCriticalValue;

    /** A map where each key is a frequent itemset and the value is the frequent itemset's support. */
    private Map<Set<Integer>, Integer> freqItemsetToSup = new HashMap<>();

    /**
     * A map where each key is a significant frequent itemset and the value is the support and p-value
     * for that significant frequent itemset.
     */
    private final Map<Set<Integer>, SupAndPvalue> sigFreqItemsetToSupAndPvalue = new HashMap<>();

    /** The runtime to get the minimum p-values. */
    private long getMinPvaluesTime = 0;

    /** The runtime to set the adjusted critical value. */
    private long setAdjustedCriticalValueTime = 0;

    /** The runtime to mine the significant frequent itemsets. */
    private long mineSigFreqItemsetsTime = 0;

    public static void main(String[] args) {
        System.out.println("Reading configuration file at " + args[0]);

        final JSONObject conf = JsonFile.read(args[0]);
        final String datasetPath = conf.getString(JsonKeys.datasetPath);
        final int numSwaps = conf.getInt(JsonKeys.numSwaps);
        final int numEstSamples = conf.getInt(JsonKeys.numEstSamples);
        final int numWySamples = conf.getInt(JsonKeys.numWySamples);
        final double minFreq = conf.getDouble(JsonKeys.minFreq);
        final double fwer = conf.getDouble(JsonKeys.fwer);
        final int numThreads = conf.getInt(JsonKeys.numThreads);
        long seed = conf.getLong(JsonKeys.seed);
        final String resultsDir = conf.getString(JsonKeys.resultsDir);
        final boolean cleanup = conf.getBoolean(JsonKeys.cleanup);

        final String samplerType = args[1];
        final Sampler sampler = Sampler.getNewSampler(samplerType);

        if (seed == -1) {
            Random rnd = new Random();
            seed = rnd.nextLong();
        }

        SigFreqItemsetProcessing processing = new SigFreqItemsetProcessing(
                datasetPath,
                sampler,
                numSwaps,
                numEstSamples,
                numWySamples,
                minFreq,
                fwer,
                numThreads,
                seed,
                Paths.concat(resultsDir, samplerType),
                cleanup
        );
        processing.process();
    }

    public SigFreqItemsetProcessing(
            String datasetPath,
            Sampler sampler,
            int numSwaps,
            int numEstSamples,
            int numWySamples,
            double minFreq,
            double fwer,
            int numThreads,
            long seed,
            String resultsDir,
            boolean cleanup) {
        this.sampler = sampler;
        this.numSwaps = numSwaps;
        this.numEstSamples = numEstSamples;
        this.numWySamples = numWySamples;
        this.minFreq = minFreq;
        this.fwer = fwer;
        this.numThreads = numThreads;
        this.seed = seed;
        this.paths = new Paths(datasetPath, resultsDir);
        this.cleanup = cleanup;
    }

    /** Runs the algorithm the obtain the set of significant frequent itemsets. */
    public void process() {
        System.out.println("Mining significant frequent itemset miner with arguments:");
        System.out.println("\t" + JsonKeys.datasetPath + ": " + this.paths.datasetPath);
        //System.out.println("\t" + JsonKeys.sampler + ": " + Sampler.getName(this.sampler));
        System.out.println("\t" + JsonKeys.numSwaps + ": " + this.numSwaps);
        System.out.println("\t" + JsonKeys.numEstSamples + ": " + this.numEstSamples);
        System.out.println("\t" + JsonKeys.numWySamples + ": " + this.numWySamples);
        System.out.println("\t" + JsonKeys.minFreq + ": " + this.minFreq);
        System.out.println("\t" + JsonKeys.fwer + ": " + this.fwer);
        System.out.println("\t" + JsonKeys.numThreads + ": " + this.numThreads);
        System.out.println("\t" + JsonKeys.seed + ": " + this.seed);
        System.out.println("\t" + JsonKeys.resultsDir + ": " + this.paths.resultsDir);
        System.out.println("\t" + JsonKeys.cleanup + ": " + this.cleanup);

        System.out.println("Getting minimum p-values");
        final long getMinPvaluesTimeStart = System.currentTimeMillis();
        this.setMinPvalues();
        this.getMinPvaluesTime = System.currentTimeMillis() - getMinPvaluesTimeStart;

        final long setAdjustedCriticalValueTimeStart = System.currentTimeMillis();
        this.setAdjustedCriticalValue();
        this.setAdjustedCriticalValueTime =
                System.currentTimeMillis() - setAdjustedCriticalValueTimeStart;
        System.out.println("Adjusted critical value: " + this.adjustedCriticalValue);

        // if adjusted critical value is equal to minimum possible value
        if (this.adjustedCriticalValue == Itemsets.getPvalue(0, this.numEstSamples)) {
            System.out.println("No significant frequent itemsets found");
        } else {
            System.out.println("Mining significant frequent itemsets");
            final long mineSigFreqItemsetsTimeStart = System.currentTimeMillis();
            this.mineSigFreqItemsets();
            this.mineSigFreqItemsetsTime = System.currentTimeMillis() - mineSigFreqItemsetsTimeStart;
        }

        this.saveResults();

        if (cleanup) {
            cleanup();
        }
    }

    // TODO: See if we can add the processing from NumFreqItemsetsExperiment.java (may have shared file reading and computations)

    /** Removes the directories containing the samples and frequent itemsets. */
    private void cleanup() {
        System.out.println("Deleting " + this.paths.samplesPath);
        Paths.deleteDir(this.paths.samplesPath);
        System.out.println("Deleting frequent itemsets from " + this.paths.freqItemsetsDirPath);
        Paths.deleteDir(this.paths.freqItemsetsDirPath);
    }

    /** Computes and sets the minimum p-values in parallel. */
    private void setMinPvalues() {
        this.minPvalues = new double[this.numWySamples];

        final ExecutorService pool = Executors.newFixedThreadPool(this.numThreads);

        for (int i = 0; i < this.numWySamples; i++) {
            final String wyFreqItemsetsPath = this.paths.getFreqItemsetsPath(Paths.wyTag, i);
            final MinPvaluesTask minPvalueTask =
                    new MinPvaluesTask(
                            this.paths, wyFreqItemsetsPath, this.numEstSamples, this.minPvalues, i);
            pool.execute(minPvalueTask);
        }

        pool.shutdown();
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            System.err.println("Error executing minimum p-values tasks");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /** Computes and sets the adjusted critical value. */
    private void setAdjustedCriticalValue() {
        Arrays.sort(this.minPvalues);
        this.adjustedCriticalValue = this.minPvalues[(int) (this.numWySamples * this.fwer)];
    }

    /** Mines the set of significant frequent itemsets. */
    private void mineSigFreqItemsets() {
        final String datasetPath = this.paths.datasetPath;

        this.freqItemsetToSup = FreqItemsetMiner.mine(datasetPath, this.minFreq);
        System.out.println("Number of frequent itemsets: " + this.freqItemsetToSup.size());

        final Map<Set<Integer>, Double> freqItemsetToPvalue =
                Itemsets.getFreqItemsetToPvalueMap(this.paths, this.freqItemsetToSup, this.numEstSamples);

        for (Entry<Set<Integer>, Double> entry : freqItemsetToPvalue.entrySet()) {
            final double pvalue = entry.getValue();
            if (pvalue <= this.adjustedCriticalValue) {
                final Set<Integer> itemset = entry.getKey();
                final int sup = this.freqItemsetToSup.get(itemset);
                this.sigFreqItemsetToSupAndPvalue.put(itemset, new SupAndPvalue(sup, pvalue));
            }
        }
        System.out.println(
                "Number of significant frequent itemsets: " + this.sigFreqItemsetToSupAndPvalue.size());
    }

    /** Saves the results to disk. */
    private void saveResults() {
        // create object for frequent itemsets
        final JSONObject freqItemsetsJson = new JSONObject();
        for (Entry<Set<Integer>, Integer> entry : this.freqItemsetToSup.entrySet()) {
            final Set<Integer> freqItemset = entry.getKey();
            final int sup = entry.getValue();

            final String freqItemsetString = Itemsets.toString(freqItemset);

            final JSONObject supJson = new JSONObject();
            supJson.put(JsonKeys.sup, sup);

            freqItemsetsJson.put(freqItemsetString, supJson);
        }

        // create object for significant frequent itemsets
        final JSONObject sigFreqItemsetsJson = new JSONObject();
        for (Entry<Set<Integer>, SupAndPvalue> entry : this.sigFreqItemsetToSupAndPvalue.entrySet()) {
            final Set<Integer> sigFreqItemset = entry.getKey();
            final SupAndPvalue supAndPvalue = entry.getValue();
            final int sup = supAndPvalue.sup;
            final double pvalue = supAndPvalue.pvalue;

            final String sigFreqItemsetString = Itemsets.toString(sigFreqItemset);

            final JSONObject supAndPvalueJson = new JSONObject();
            supAndPvalueJson.put(JsonKeys.sup, sup);
            supAndPvalueJson.put(JsonKeys.pvalue, pvalue);

            sigFreqItemsetsJson.put(sigFreqItemsetString, supAndPvalueJson);
        }

        // create object for itemsets
        final JSONObject itemsets = new JSONObject();
        itemsets.put(JsonKeys.freqItemsets, freqItemsetsJson);
        itemsets.put(JsonKeys.sigFreqItemsets, sigFreqItemsetsJson);

        // create object for args
        final JSONObject args = new JSONObject();
        args.put(JsonKeys.datasetPath, this.paths.datasetPath);
        //args.put(JsonKeys.sampler, Sampler.getName(this.sampler));
        args.put(JsonKeys.numSwaps, this.numSwaps);
        args.put(JsonKeys.numEstSamples, this.numEstSamples);
        args.put(JsonKeys.numWySamples, this.numWySamples);
        args.put(JsonKeys.minFreq, this.minFreq);
        args.put(JsonKeys.fwer, this.fwer);
        args.put(JsonKeys.numThreads, this.numThreads);
        args.put(JsonKeys.seed, this.seed);

        // create object for runtimes
        final JSONObject runtimes = new JSONObject();
        runtimes.put(JsonKeys.getMinPvaluesTime, this.getMinPvaluesTime);
        runtimes.put(JsonKeys.setAdjustedCriticalValueTime, this.setAdjustedCriticalValueTime);
        runtimes.put(JsonKeys.mineSigFreqItemsetsTime, this.mineSigFreqItemsetsTime);

        // create object for runInfo
        final JSONObject runInfo = new JSONObject();
        runInfo.put(JsonKeys.args, args);
        runInfo.put(JsonKeys.runtimes, runtimes);
        runInfo.put(JsonKeys.timestamp, LocalDateTime.now());
        runInfo.put(JsonKeys.adjustedCriticalValue, this.adjustedCriticalValue);
        runInfo.put(JsonKeys.numFreqItemsets, this.freqItemsetToSup.size());
        runInfo.put(JsonKeys.numSigFreqItemsets, this.sigFreqItemsetToSupAndPvalue.size());
        runInfo.put(JsonKeys.minPvalues, this.minPvalues);

        // create base object
        final JSONObject results = new JSONObject();
        results.put(JsonKeys.runInfo, runInfo);
        results.put(JsonKeys.itemsets, itemsets);

        // save JSON
        final String sigFreqItemsetsPath =
                this.paths.getSigFreqItemsetsPath(
                        this.sampler,
                        this.numSwaps,
                        this.numEstSamples,
                        this.numWySamples,
                        this.minFreq,
                        this.fwer,
                        this.numThreads,
                        this.seed);
        JsonFile.write(results, sigFreqItemsetsPath);

        System.out.println("Results written to " + sigFreqItemsetsPath);
    }
}
