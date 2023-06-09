package rohan.io;

/*
 * Copyright (C) 2022 Alexander Lee and Matteo Riondato
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
import rohan.samplers.GmmtSampler;
import rohan.samplers.NaiveSampler;
import rohan.samplers.RefinedSampler;

/** A class for the names of the samplers we use. */
public class SamplerNames {
  public static final String gmmtSampler = GmmtSampler.class.getName();
  public static final String naiveSampler = NaiveSampler.class.getName();
  public static final String refinedSampler = RefinedSampler.class.getName();
}
