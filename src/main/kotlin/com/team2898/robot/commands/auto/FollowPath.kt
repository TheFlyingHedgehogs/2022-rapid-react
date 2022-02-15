package com.team2898.robot.commands.auto

import com.bpsrobotics.engine.utils.TrajectoryUtils.endPose
import com.bpsrobotics.engine.utils.deg
import com.bpsrobotics.engine.utils.m
import com.team2898.robot.subsystems.Drivetrain
import com.team2898.robot.subsystems.Odometry
import edu.wpi.first.math.trajectory.Trajectory
import edu.wpi.first.wpilibj2.command.CommandBase

/**
 * Commands the drivetrain to follow a path.
 *
 * @param path: Path Planner Trajectory to be run
 * @param resetOdometry: Reset Odometry to path initial state
 */
class FollowPath(private val path: Trajectory, private val resetOdometry: Boolean = false) : CommandBase() {
    private var leftStart = false

    override fun initialize() {
        if (resetOdometry) {
            Odometry.reset(path.initialPose.x.m, path.initialPose.y.m, (-path.initialPose.rotation.degrees).deg)
        }
        Drivetrain.follow(path)
    }

    override fun isFinished(): Boolean {
        return if (leftStart) {
            path.endPose.translation.getDistance(Odometry.pose.translation) < 0.1
        }
        else {
            if (path.initialPose.translation.getDistance(Odometry.pose.translation) > 0.25) {
                leftStart = true
            }
            false
        }
    }

    override fun end(interrupted: Boolean) {
        if (interrupted) {
            Drivetrain.stopAuto()
        }
    }
}