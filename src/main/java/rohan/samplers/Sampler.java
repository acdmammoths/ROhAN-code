package rohan.samplers;

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
/** An interface for different dataset samplers to implement. */
import rohan.experiments.Timer;
import rohan.io.SamplerNames;
import rohan.structures.SparseMatrix;

public interface Sampler {
  public SparseMatrix sample(SparseMatrix inMatrix, int numSwaps, long seed, Timer timer);

  public static Sampler getNewSampler(String samplerType) {
    Sampler sampler = null;
    samplerType = samplerType.toLowerCase();
    if (SamplerNames.gmmtSampler.toLowerCase().endsWith(samplerType)) {
      sampler = new GmmtSampler();
    } else if (SamplerNames.naiveSampler.toLowerCase().endsWith(samplerType)) {
      sampler = new NaiveSampler();
    } else if (SamplerNames.refinedSampler.toLowerCase().endsWith(samplerType)) {
      sampler = new RefinedSampler();
    }
    return sampler;
  }

  public static String getName(Sampler sampler){
    String[] parts = sampler.getClass().getName().split("\\.");
    return parts[parts.length - 1];
  }
}
