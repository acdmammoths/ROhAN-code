package rohan.mining;

/*
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
import rohan.experiments.Timer;
import rohan.io.*;
import rohan.samplers.Sampler;
import rohan.structures.SparseMatrix;
import org.json.JSONObject;

import java.util.Random;

/**
 * A class that helps sample transactional datasets and mines frequent itemsets from those datasets
 * in parallel.
 */
public class SampleAndMineTask implements Runnable {
  /** The sampler used to sample matrices. */
  private final Sampler sampler;

  /** The transformer to create the dataset from the matrix. */
  private final Transformer transformer;

  /** The matrix of the observed dataset. */
  private final SparseMatrix matrix;

  /** The number of swaps to perform. */
  private final int numSwaps;

  /** The random seed to use for replication. */
  private final long seed;

  /** The minimum frequency threshold used to mine the set of frequent itemsets. */
  private final double minFreq;

  /** The path to save the sampled transactional dataset. */
  private final String samplePath;

  /** The path to save the set of frequent itemsets. */
  private final String freqItemsetsPath;

  /** The flag for whether to sample. */
  private boolean mineOnly = false;



  public static void main(String[] args) {
    final String confPath = args[0];
    final String samplePath = args[1];
    final String freqItemsetsPath = args[2];
    final String samplerType = args[3];
    Sampler samplerLocal = Sampler.getNewSampler(samplerType);
    if (samplerLocal == null){
      System.err.println("Unknown sample type " + samplerType);
      System.exit(1);
    }

    System.out.println("Reading configuration file at " + confPath);

    final JSONObject conf = JsonFile.read(confPath);
    final String datasetPath = conf.getString(JsonKeys.datasetPath);
    final int numSwaps = conf.getInt(JsonKeys.numSwaps);
    final double minFreq = conf.getDouble(JsonKeys.minFreq);
    final boolean mineOnly = conf.getBoolean("mineOnly");
    long seed = conf.getLong(JsonKeys.seed);
    if (seed == -1){
      Random rnd = new Random();
      seed = rnd.nextLong();
    }

    System.out.println("Mining significant frequent itemset miner with arguments:");
    System.out.println("\t" + JsonKeys.datasetPath + ": " + datasetPath);
    System.out.println("\t" + JsonKeys.sampler + ": " + Sampler.getName(samplerLocal));
    System.out.println("\t" + JsonKeys.numSwaps + ": " + numSwaps);
    System.out.println("\t" + JsonKeys.minFreq + ": " + minFreq);
    System.out.println("\t" + JsonKeys.seed + ": " + seed);
    System.out.println("\t" + JsonKeys.samplePath + ": " + samplePath);
    System.out.println("\t" + JsonKeys.freqItemsetsPath + ": " + freqItemsetsPath);

    final long totalRuntimeStart = System.currentTimeMillis();

    System.out.println("Creating matrix from dataset");
    final long createMatrixTimeStart = System.currentTimeMillis();
    final Transformer transformer = new Transformer();
    final SparseMatrix matrix = transformer.createMatrix(datasetPath);
    final long createMatrixTime = System.currentTimeMillis() - createMatrixTimeStart;

    System.out.println("Creating task and running");
    final long taskTimeStart = System.currentTimeMillis();

    final SampleAndMineTask sampleTask =
            new SampleAndMineTask(
                    samplerLocal,
                    transformer,
                    matrix,
                    numSwaps,
                    seed,
                    minFreq,
                    samplePath,
                    freqItemsetsPath,
                    mineOnly);

    sampleTask.run();

    final long taskTime = System.currentTimeMillis() - taskTimeStart;
    System.out.println("\t" + JsonKeys.taskTime + ": " + taskTime);
  }

  SampleAndMineTask(
      Sampler sampler,
      Transformer transformer,
      SparseMatrix matrix,
      int numSwaps,
      long seed,
      double minFreq,
      String samplePath,
      String freqItemsetsPath) {
    this.sampler = sampler;
    this.transformer = transformer;
    this.matrix = matrix;
    this.numSwaps = numSwaps;
    this.seed = seed;
    this.minFreq = minFreq;
    this.samplePath = samplePath;
    this.freqItemsetsPath = freqItemsetsPath;
  }

  SampleAndMineTask(
          Sampler sampler,
          Transformer transformer,
          SparseMatrix matrix,
          int numSwaps,
          long seed,
          double minFreq,
          String samplePath,
          String freqItemsetsPath,
          boolean mineOnly) {
    this.sampler = sampler;
    this.transformer = transformer;
    this.matrix = matrix;
    this.numSwaps = numSwaps;
    this.seed = seed;
    this.minFreq = minFreq;
    this.samplePath = samplePath;
    this.freqItemsetsPath = freqItemsetsPath;
    this.mineOnly = mineOnly;
  }

  @Override
  public void run() {
    if (!this.mineOnly) {
      final SparseMatrix sample =
              this.sampler.sample(this.matrix, this.numSwaps, this.seed, new Timer(false));
      this.transformer.createDataset(this.samplePath, sample);
      System.out.println("Sample created: " + this.samplePath);
    }

    FreqItemsetMiner.mine(this.samplePath, this.minFreq, this.freqItemsetsPath);
    System.out.println("Frequent itemsets mined: " + this.freqItemsetsPath);
  }
}
