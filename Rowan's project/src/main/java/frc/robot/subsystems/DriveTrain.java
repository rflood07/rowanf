package frc.robot.subsystems;

import static frc.robot.Constants.DriveConstants.BL_DRIVE_MOTOR_ID;
import static frc.robot.Constants.DriveConstants.BR_DRIVE_MOTOR_ID;
import static frc.robot.Constants.DriveConstants.FL_DRIVE_MOTOR_ID;
import static frc.robot.Constants.DriveConstants.FR_DRIVE_MOTOR_ID;
import static frc.robot.Constants.DriveConstants.BL_STEER_MOTOR_ID;

import java.util.Arrays;
import java.util.Collections;

import com.ctre.phoenixpro.hardware.Pigeon2;
import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.CTREConfigs;
import frc.robot.Constants.DriveConstants;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.Preferences;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import static frc.robot.Constants.DriveConstants.*;

public class DriveTrain extends SubsystemBase{
    public static final CTREConfigs ctreConfig = new CTREConfigs();
    public SwerveDriveKinematics kinematics = DriveConstants.kinematics;

    private final Pigeon2 pigeon = new Pigeon2(DRIVETRAIN_PIGEON_ID, CANIVORE_DRIVETRAIN);

    private final SwerveModule[] modules;
    private Rotation2d[] lastAngles;

    private final SwerveDrivePoseEstimator odometer;
    private final Field2d m_field = new Field2d();

    public DriveTrain() {

        Preferences.initString("FR", "AUGIE");
        Preferences.initString("FL", "AUGIE");
        Preferences.initString("BR", "AUGIE");
        Preferences.initString("BL", "AUGIE");

        SmartDashboard.putData("Field", m_field);
        SmartDashboard.putData("resetOdemetry", new InstantCommand(()-> this.resetOdemetry()));
        modules = new SwerveModule[4];
        lastAngles = new Rotation2d[] {new Rotation2d(), new Rotation2d(), new Rotation2d(), new Rotation2d()};

        modules[0] = new SwerveModule(
            "FL",
            FL_DRIVE_MOTOR_ID,
            FL_STEER_MOTOR_ID,
            FL_STEER_ENCODER_ID,
            Offsets.valueOf(Preferences.getString("FL", "AUGIE")).getValue(Positions.FL),
            CANIVORE_DRIVETRAIN);
        modules[1] = new SwerveModule(
            "FR",
            FR_DRIVE_MOTOR_ID,
            FR_STEER_MOTOR_ID,
            FR_STEER_ENCODER_ID,
            Offsets.valueOf(Preferences.getString("FR", "AUGIE")).getValue(Positions.FR),
            CANIVORE_DRIVETRAIN);
        modules[2] = new SwerveModule(
            "BL",
            BL_DRIVE_MOTOR_ID,
            BL_STEER_MOTOR_ID,
            BL_STEER_ENCODER_ID,
            Offsets.valueOf(Preferences.getString("BL", "AUGIE")).getValue(Positions.BL),
            CANIVORE_DRIVETRAIN);
        modules[3] = new SwerveModule(
            "BR",
            BR_DRIVE_MOTOR_ID,
            BR_STEER_MOTOR_ID,
            BR_STEER_ENCODER_ID,
            Offsets.valueOf(Preferences.getString("BR", "AUGIE")).getValue(Positions.BR),
            CANIVORE_DRIVETRAIN);

        odometer = new SwerveDrivePoseEstimator
        (kinematics,
        getGyroscopeRotation(),
        getModulePositions(),
        DRIVE_ODOMETRY_ORIGIN);
    }

public void zeroGyroscope() {
    pigeon.setYaw(0.0);
    odometer.resetPosition(new Rotation2d(), getModulePositions(), new Pose2d(getPose().getTranslation(), new Rotation2d()));
}
public void zeroGyroscope(double angleDeg) {
    pigeon.setYaw(angleDeg);
    new Rotation2d();
    new Rotation2d();
    odometer.resetPosition(Rotation2d.fromDegrees(angleDeg), getModulePositions(), new Pose2d(getPose().getTranslation(), Rotation2d.fromDegrees(angleDeg)));
}
public Rotation2d getGyroscopeRotation() {
    return pigeon.getRotation2d();
}
public SwerveModulePosition[] getModulePositions() {
    SwerveModulePosition[] positions = new SwerveModulePosition[4];
    for (int i = 0; i < 4; i++) positions[i] = modules[i].getPosition();
    return positions;
}
public SwerveModuleState[] getModuleStates() {
    SwerveModuleState[] states = new SwerveModuleState[4];
    for (int i = 0; i < 4; i++) states[i] = modules[i].getState();
    return states;
}
public Pose2d getPose() {
    return odometer.getEstimatedPosition();
}
public void resetOdemetry(Pose2d pose) {
    zeroGyroscope(pose.getRotation().getDegrees());
    odometer.resetPosition(pose.getRotation(), getModulePositions(), pose);
}
public void resetOdemetry() {
    odometer.resetPosition(getGyroscopeRotation(), getModulePositions(), DRIVE_ODOMETRY_ORIGIN);
}

public void pointWheelsForward() {
    for (int i = 0; i < 4; i++) {
    setModule(i, new SwerveModuleState(0, new Rotation2d()));
}
}
public void pointWheelsInward() {
    setModule(0, new SwerveModuleState(0, Rotation2d.fromDegrees(-135)));
    setModule(1, new SwerveModuleState(0, Rotation2d.fromDegrees(135)));
    setModule(2, new SwerveModuleState(0, Rotation2d.fromDegrees(-45)));
    setModule(3, new SwerveModuleState(0, Rotation2d.fromDegrees(45)));
}
public void drive(ChassisSpeeds chassisSpeeds) {
    SwerveModuleState[] desiredStates = kinematics.toSwerveModuleStates(chassisSpeeds);
    double maxSpeed = Collections.max(Arrays.asList(desiredStates)).speedMetersPerSecond;
    if (maxSpeed <= DriveConstants.DRIVE_DEADBAND_MPS) {
        stop();
    } else {
        setModuleStates(desiredStates);
    }
}
public void stop() {
    for (int i = 0; i < 4; i++) {
        modules[i].setDesiredState(new SwerveModuleState(0, lastAngles[i]));
    }
}
public void setModuleStates(SwerveModuleState[] desiredStates) {
    SwerveDriveKinematics.desaturateWheelSpeeds(desiredStates, DriveConstants.MAX_VELOCITY_METERS_PER_SECOND);
    for (int i = 0; i < 4; i++) {
        setModule(i, desiredStates[i]);
    }
}
private void setModule(int i, SwerveModuleState desiredState) {
    modules[i].setDesiredState(desiredState);
    lastAngles[i] = desiredState.angle;
}
public void updateOdometry() {
    odometer.updateWithTime(Timer.getFPGATimestamp(), getGyroscopeRotation(), getModulePositions());
}
public void periodic() {
    updateOdometry();
    m_field.setRobotPose(odometer.getEstimatedPosition());
    SmartDashboard.putNumber("Robot Angle", getGyroscopeRotation().getDegrees());
    SmartDashboard.putString("Robot Location", getPose().getTranslation().toString());
}
}