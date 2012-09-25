package net.androgames.level.config;

import net.androgames.level.R;

/*
 *  This file is part of Level (an Android Bubble Level).
 *  <https://github.com/avianey/Level>
 *  
 *  Copyright (C) 2012 Antoine Vianey
 *  
 *  Level is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Level is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Level. If not, see <http://www.gnu.org/licenses/>
 */
public enum Viscosity {

	HIGH(R.string.viscosity_high_summary, 0.5d),
	MEDIUM(R.string.viscosity_medium_summary, 1d),
	LOW(R.string.viscosity_low_summary, 1.5d);

	private int summary;
	private double coeff;
	
	private Viscosity(int summary, double coeff) {
		this.summary = summary;
		this.coeff = coeff;
	}
	
	public double getCoeff() {
		return coeff;
	}

	public int getSummary() {
		return summary;
	}

}
