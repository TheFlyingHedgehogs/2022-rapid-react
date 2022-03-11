package com.team2898.robot.subsystems

import com.bpsrobotics.engine.utils.Interpolation
import com.bpsrobotics.engine.utils.RPM
import com.bpsrobotics.engine.utils.plus
import com.bpsrobotics.engine.utils.seconds
import com.revrobotics.CANSparkMax
import com.revrobotics.CANSparkMax.IdleMode.kCoast
import com.revrobotics.CANSparkMaxLowLevel.MotorType.kBrushless
import com.team2898.robot.Constants.DUMP_SPEED
import com.team2898.robot.RobotMap.SHOOTER_FLYWHEEL
import com.team2898.robot.RobotMap.SHOOTER_SPINNER
import edu.wpi.first.util.sendable.SendableBuilder
import edu.wpi.first.wpilibj.Timer
import edu.wpi.first.wpilibj2.command.SubsystemBase

object Shooter : SubsystemBase() {
    private val flywheelController = CANSparkMax(SHOOTER_FLYWHEEL, kBrushless)
    private val spinnerController = CANSparkMax(SHOOTER_SPINNER, kBrushless)
    private var spunUpTime = 0.seconds
    val ready
        get() =
            if (shooterPower.flywheel > 0.4 && shooterPower.spinner > 0.6) {
                flywheelController.encoder.velocity > 2000 || spinnerController.encoder.velocity > 3200 // TODO
            } else if (shooterPower.flywheel > 0.05 && shooterPower.spinner > 0.75) {
                flywheelController.encoder.velocity > 300 && spinnerController.encoder.velocity > 4000
            } else {
                flywheelController.encoder.velocity > 800 && spinnerController.encoder.velocity > 800
            }

//        (flywheelController.encoder.velocity - target.flywheel.value).absoluteValue < 10 &&
//                (spinnerController.encoder.velocity - target.spinner.value).absoluteValue < 10 &&
//                min(abs(flywheelController.encoder.velocity), abs(spinnerController.encoder.velocity)) > 10
    var target = ShooterSpeeds(0.RPM, 0.RPM)
    var shooterPower = ShooterPowers(0.0, 0.0)

    data class ShooterSpeeds(val flywheel: RPM, val spinner: RPM)
    data class ShooterPowers(val flywheel: Double, val spinner: Double)

    init {
        listOf(flywheelController, spinnerController).forEach {
            it.restoreFactoryDefaults()
            it.setSmartCurrentLimit(20)
            it.idleMode = kCoast
            it.inverted = true
        }

//        flywheelController.pidController.ff = 0.13213 / 12
//        flywheelController.pidController.p = 0.0
//        flywheelController.pidController.i = 0.0
//        flywheelController.pidController.d = 0.0
//
//        spinnerController.pidController.ff = 0.12945 / 12
//        spinnerController.pidController.p = 0.0
//        spinnerController.pidController.i = 0.0
//        spinnerController.pidController.d = 0.0
    }

    fun spinUp(speeds: ShooterPowers = Interpolation.getPowers()) {
        spunUpTime = Timer.getFPGATimestamp().seconds + 5.seconds
        shooterPower = speeds
        notMaxSpeed()
    }

    fun dumpSpinUp() {
        spinUp(DUMP_SPEED)
    }

    fun stopShooter() {
        spunUpTime = Timer.getFPGATimestamp().seconds
        notMaxSpeed()
    }

    private var maxSpeed = false

    fun maxSpeed() {
        listOf(flywheelController, spinnerController).forEach {
            it.setSmartCurrentLimit(40)
        }
        maxSpeed = true
    }

    fun notMaxSpeed() {
        if (!maxSpeed) return
        listOf(flywheelController, spinnerController).forEach {
            it.setSmartCurrentLimit(20)
        }
        maxSpeed = false
    }

    override fun periodic() {
        if (maxSpeed) {
            flywheelController.set(1.0)
            spinnerController.set(1.0)
        } else if (Timer.getFPGATimestamp() < spunUpTime.value) {
            flywheelController.setVoltage(shooterPower.flywheel * 12)
            spinnerController.setVoltage(shooterPower.spinner * 12)
//            flywheelController.setVoltage(0.44 * 12)
//            spinnerController.setVoltage(0.65 * 12)
//            flywheelController.pidController.setReference(target.flywheel.value, kVelocity)
//            spinnerController.pidController.setReference(target.spinner.value, kVelocity)
        } else {
            flywheelController.set(0.0)
            spinnerController.set(0.0)
        }
    }

    override fun initSendable(builder: SendableBuilder) {
        builder.setSmartDashboardType("Subsystem")
        builder.addDoubleProperty("shooter RPM", { flywheelController.encoder.velocity }) {}
        builder.addDoubleProperty("spinner RPM", { spinnerController.encoder.velocity }) {}
        builder.addDoubleProperty("shooter target", { target.flywheel.value }) {}
        builder.addDoubleProperty("spinner target", { target.spinner.value }) {}
    }
}
