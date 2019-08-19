/*
 * Copyright (c) 2019 Jannis Scheibe <jannis@tadris.de>
 *
 * This file is part of FitoTrack
 *
 * FitoTrack is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     FitoTrack is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.tadris.fitness.util;

import de.tadris.fitness.R;
import de.tadris.fitness.data.Workout;

public class WorkoutTypeCalculator {

    public static int getType(Workout workout){
        if(workout.workoutType.equals(Workout.WORKOUT_TYPE_RUNNING)){
            if(workout.avgSpeed < 7){
                return R.string.workoutTypeWalking;
            }else if(workout.avgSpeed < 9.6){
                return R.string.workoutTypeJogging;
            }else{
                return R.string.workoutTypeRunning;
            }
        }
        if(workout.workoutType.equals(Workout.WORKOUT_TYPE_CYCLING)){
            return R.string.workoutTypeCycling;
        }
        if(workout.workoutType.equals(Workout.WORKOUT_TYPE_HIKING)){
            return R.string.workoutTypeHiking;
        }
        return R.string.workoutTypeUnknown;
    }

}
