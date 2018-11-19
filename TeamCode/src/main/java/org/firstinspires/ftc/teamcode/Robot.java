package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.Func;
import org.firstinspires.ftc.robotcore.external.Telemetry;

public class Robot {
    public static final String ROBOT_TAG = "team14821";
    HardwareMap hardwareMap;
    BaseLinearOpMode opMode;
    DcMotor M0;
    DcMotor M1;
    DcMotor armExtensionMotor;
    DcMotor hookMotor;
    DcMotor swingMotor;
    Servo grabberServo;
    Servo mineralPlowServo;

    boolean safetysAreDisabled = false;

    boolean motorsInFront = true;

    // Degrees turned to the right are negative
    double totalDegreesTurned = 0;
    double lastHeading = 0;

    // Encoder when arm extension is at the top
    boolean armExtensionCalibrated = false;
    double extension0;
    // Encoder when arm swing is at the bottom
    boolean armSwingCalibrated = false;
    double swing0;
    // Encoder when hook is at bottom
    boolean hookCalibrated = false;
    double hook0;
    // Max height of hook (hook0 + MAX_HOOK_DISTANCE)
    public final double MAX_HOOK_DISTANCE = 27500;
    public final double MAX_SWING_ARM_DISTANCE = 2300;
    public final double MAX_ARM_EXTENSION_DISTANCE = 12800;


    TeamImu teamImu;

    // Telemetry
    String drivingCommand = "";

    public static final double ENCODER_CLICKS_PER_INCH = 79.27;


    public Robot(BaseLinearOpMode baseLinearOpMode, HardwareMap hardwareMap, Telemetry telemetry) {
        this.hardwareMap = hardwareMap;
        opMode = baseLinearOpMode;
        M0 = hardwareMap.dcMotor.get("M0");
        M1 = hardwareMap.dcMotor.get("M1");

        mineralPlowServo = hardwareMap.servo.get("MineralPlowServo");
        //mineralPlowServo.setPosition(0);
        grabberServo = hardwareMap.servo.get("GrabberServo");
        hookMotor = hardwareMap.dcMotor.get("HookMotor");
        armExtensionMotor = hardwareMap.dcMotor.get("ArmExtensionMotor");
        armExtensionMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        hookMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        swingMotor = hardwareMap.dcMotor.get("SwingMotor");
        swingMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        swingMotor.setDirection(DcMotor.Direction.REVERSE);

        teamImu = new TeamImu().initialize(hardwareMap, telemetry);

        totalDegreesTurned = 0;
        lastHeading = getImuHeading();

        setRobotOrientation(true);
        stop();

        setupRobotTelemetry(telemetry);
    }

    boolean shouldRobotKeepRunning() {
        return opMode.shouldOpModeKeepRunning();
    }

    private void setupRobotTelemetry(Telemetry telemetry) {
        telemetry.addLine("Robot: ")
                .addData("TotDeg", new Func<String>() {
                    @Override
                    public String value() {
                        return String.format("%.1f", totalDegreesTurned);
                    }
                })
                .addData("Cmd", new Func<String>() {
                    @Override
                    public String value() {
                        return drivingCommand;
                    }
                })
                .addData("Orientation", new Func<String>() {
                    @Override
                    public String value() {
                        return motorsInFront ? "MotorsFront" : "MotorsBack";
                    }
                })
        ;

        telemetry.addLine("Motors: ")
                .addData("L", new Func<String>() {
                    @Override
                    public String value() {
                        return String.format("%.1f@%d", getLeftMotor().getPower(), getLeftMotor().getCurrentPosition());
                    }
                })
                .addData("R", new Func<String>() {
                    @Override
                    public String value() {
                        return String.format("%.1f@%d", getRightMotor().getPower(), getRightMotor().getCurrentPosition());
                    }
                })
                .addData("M0", new Func<String>() {
                    @Override
                    public String value() {
                        return String.format("%.1f@%d", M0.getPower(), M0.getCurrentPosition());
                    }
                })
                .addData("M1", new Func<String>() {
                    @Override
                    public String value() {
                        return String.format("%.1f@%d", M1.getPower(), M1.getCurrentPosition());
                    }
                })
                .addData("Plow", new Func<String>() {
                    @Override
                    public String value() {
                        return String.format("%.1f", mineralPlowServo.getPosition());
                    }
                })
                .addData("Hook", new Func<String>() {
                    @Override
                    public String value() {
                        return String.format("%.1f@%d", hookMotor.getPower(), hookMotor.getCurrentPosition());
                    }
                })
                .addData("Arm Extension", new Func<String>() {
                    @Override
                    public String value() {
                        return String.format("%.1f@%d", armExtensionMotor.getPower(), armExtensionMotor.getCurrentPosition());
                    }
                })
                .addData("Arm Swing", new Func<String>() {
                    @Override
                    public String value() {
                        return String.format("%.1f@%d", swingMotor.getPower(), swingMotor.getCurrentPosition());
                    }
                })
        ;
    }


    public void stop() {
        drivingCommand = "stop";
        setRightPower(0);
        setLeftPower(0);
    }

    public void driveStraight(double power) {
        setDrivingCommand("Straight(%.2f)", power);

        setLeftPower(power);
        setRightPower(power);

    }

    private void setDrivingCommand(String format, Object... args) {
        String newDrivingCommand = String.format(format, args);
        if ( !drivingCommand.equals(newDrivingCommand))
            RobotLog.ww("team14821", "New driving command: %s", newDrivingCommand);
        drivingCommand = newDrivingCommand;
    }

    /**
     * Turn robot with opposite power to wheels
     *
     * @param power (-1..1) negative means to the left
     */
    public void spin(double power) {
        setDrivingCommand("Spin%s(%.2f)", power > 0 ? "Right" : "Left", power);
        setLeftPower(power);
        setRightPower(-power);
    }

    public void setDrivingPowers(double leftPower, double rightPower) {
        setDrivingCommand("Set(%.2f,%.2f)", leftPower, rightPower);
        setLeftPower(leftPower);
        setRightPower(rightPower);
    }

    public void setDrivingZeroPowerBehavior(DcMotor.ZeroPowerBehavior behavior) {
        M0.setZeroPowerBehavior(behavior);
        M1.setZeroPowerBehavior(behavior);
    }

    DcMotor getRightMotor() {
        if (motorsInFront) {
            return M1;
        } else {
            return M0;
        }
    }

    DcMotor getLeftMotor() {
        if (motorsInFront) {
            return M0;
        } else {
            return M1;
        }
    }

    public int getWheelPosition() {
        return M0.getCurrentPosition();
    }

    public double getTotalDegreesTurned() {
        return totalDegreesTurned;
    }

    public double getImuHeading() {
        return teamImu.getHeading();
    }


    public void reverseRobotOrientation() {
        setRobotOrientation(!motorsInFront);
    }

    public void setRobotOrientation(boolean motorsInFront) {
        RobotLog.ww("team14821", "Setting robot orientation: %s", motorsInFront ? "MotorsInFront" : "MotorsInBack");

        this.motorsInFront = motorsInFront;
        getRightMotor().setDirection(DcMotorSimple.Direction.REVERSE);
        getLeftMotor().setDirection(DcMotorSimple.Direction.FORWARD);
    }

    public void setHookPower(double power) {
        if(hookMotor.getPower() != power)
        {
            RobotLog.ww("team14821","Setting hook power to %.2f", power);
        }

        if (isHookPowerOK(power)) {
            hookMotor.setPower(power);
        }
    }


    public void setArmExtensionPower(double power) {
        if(armExtensionMotor.getPower() != power)
        {
            RobotLog.ww("team14821","Setting arm extension power to %.2f", power);
        }

        if (isArmExtensionPowerOK(power)) {
            armExtensionMotor.setPower(power);
        }
    }

    public void setSwingArm(double power) {
        if(swingMotor.getPower() != power)
        {
            RobotLog.ww("team14821","Setting swing arm power to %.2f", power);
        }

        if(isSwingArmPowerOK(power)) {
            swingMotor.setPower(power);
        }
    }

    public void setGrabberServoPosition(double position) {
        grabberServo.setPosition(position);

    }

    private void setLeftPower(double leftPower) {
        if ( leftPower != getLeftMotor().getPower() )
            RobotLog.ww("team14821", "Setting left power to %0.2f", leftPower);
        getLeftMotor().setPower(leftPower);
    }

    private void setRightPower(double rightPower) {
        if ( rightPower != getRightMotor().getPower() )
            RobotLog.ww("team14821", "Setting right power to %0.2f", rightPower);
        getRightMotor().setPower(rightPower);
    }

    /**
     * @param power    Positive power is forward, negative power is backwards (between -1, 1)
     * @param steering Positive steering is to the right, negative steering is to the left (between -1, 1)
     */
    void setPowerSteering(double power, double steering) {
        setDrivingCommand("Steer(%.2f, %.2f)", power, steering);

        double powerRight, powerLeft;

        if (steering > 0) {
            // Turning to right: powerLeft should be more than powerRight
            // Scale down powerRight
            powerRight = (1 - Math.abs(steering)) * power;
            powerLeft = power;
        } else {
            // Turning to left: powerLeft should be less than powerRight
            // Scale down powerLeft
            powerLeft = (1 - Math.abs(steering)) * power;
            powerRight = power;
        }

        setLeftPower(powerLeft);
        setRightPower(powerRight);
    }


    public void mineralPlowDown() {
        RobotLog.ww("team14821", "Setting mineral plow down");

        mineralPlowServo.setPosition(-0.3);
    }

    public void mineralPlowUp() {
        RobotLog.ww(ROBOT_TAG, "Setting mineral plow up");
        mineralPlowServo.setPosition(0);
    }

    // Protect robot
    public void healthCheck() {
        if (!isArmExtensionPowerOK(armExtensionMotor.getPower())) {
            armExtensionMotor.setPower(0);
        }

        if (!isHookPowerOK(hookMotor.getPower())) {
            hookMotor.setPower(0);
        }

        if (!isSwingArmPowerOK((swingMotor.getPower()))) {
            swingMotor.setPower(0);
        }

        // Accumulate degrees turned
        double currentHeading = getImuHeading();
        double degreesTurned = currentHeading - lastHeading;
        lastHeading = currentHeading;

        // Watch for wrap around
        if (degreesTurned > 180)
            degreesTurned -= 360;
        else if (degreesTurned < -180)
            degreesTurned += 360;

        totalDegreesTurned += degreesTurned;
    }

    private boolean isHookPowerOK(double powerToCheck) {
        boolean result;

        if(safetysAreDisabled)
            result = true;

        else if (!hookCalibrated)
            result = true;

        else if (hookMotor.getCurrentPosition() > hook0 + MAX_HOOK_DISTANCE && powerToCheck > 0) {
            result = false;
        } else if (hookMotor.getCurrentPosition() < hook0 && powerToCheck < 0) {
            result = false;
        }
        else
            result = true;

        if ( !result )
            RobotLog.ww(ROBOT_TAG, "Hook power %.2f IS NOT OKAY at position %d", powerToCheck, hookMotor.getCurrentPosition());

        return result;
    }

    private boolean isSwingArmPowerOK(double powerToCheck) {
        boolean result;

        if(safetysAreDisabled)
            result = true;

        else if (swingMotor.getCurrentPosition() > swing0 + MAX_SWING_ARM_DISTANCE && powerToCheck > 0) {
            result = false;
        } else if (swingMotor.getCurrentPosition() < swing0 && powerToCheck < 0) {
            result = false;
        }
        else
            result = true;

        if ( !result )
            RobotLog.ww(ROBOT_TAG, "Swing arm power %.2f IS NOT OKAY at position %d", powerToCheck, swingMotor.getCurrentPosition());

        return result;
    }

    private boolean isArmExtensionPowerOK(double powerToCheck) {
        boolean result;

        if(safetysAreDisabled)
            result = true;

        else if (armExtensionMotor.getCurrentPosition() > extension0 + MAX_ARM_EXTENSION_DISTANCE && powerToCheck > 0) {
            result = false;
        } else if (armExtensionMotor.getCurrentPosition() < extension0 && powerToCheck < 0) {
            result = false;
        }
        else
            result = true;

        if ( !result )
            RobotLog.ww(ROBOT_TAG, "Arm extension power %.2f IS NOT OKAY at position %d", powerToCheck, armExtensionMotor.getCurrentPosition());

        return result;
    }

    public void hookUp(double power, boolean wait) {
        setHookPower(power);
        if (wait) {
            // Wait until the safety stops the hookMotor
            while (shouldRobotKeepRunning() && hookMotor.getPower() != 0) {
            }
        }
    }

    public void hookDown(double power, boolean wait) {
        setHookPower(-Math.abs(power));
        if (wait) {
            while (shouldRobotKeepRunning() && hookMotor.getPower() != 0) {
            }
        }
    }

    public void inchmove(double inches, double power) {
        opMode.setOperation(String.format("InchMove(%.1f, %.1f", inches, power));

        double encoderClicks = inches * ENCODER_CLICKS_PER_INCH;
        double stopPosition = getWheelPosition() + encoderClicks;

        double startingHeading = getTotalDegreesTurned();

        while (shouldRobotKeepRunning() && getWheelPosition() <= stopPosition) {
            //Heading is larger to the left
            double currentHeading = getTotalDegreesTurned();
            double headingError = startingHeading - currentHeading;

            opMode.setStatus(String.format("%.1f inches to go. Heading error: %.1f degrees",
                    (stopPosition - getWheelPosition()) / ENCODER_CLICKS_PER_INCH,
                    headingError));


            if (headingError > 0.0) {
                //The current heading is too big so we turn to the left
                setPowerSteering(power, -0.05);
            } else if (headingError < 0.0) {
                //Current heading is too small, so we steer to the right
                setPowerSteering(power, 0.05);
            } else {
                // Go Straight
                driveStraight(power);
            }
        }
        stop();

        opMode.setStatus("Done");

    }


    public void turnRight(double degrees, double speed) {
        opMode.setOperation(String.format("TurnRight(d=%.1f, s=%.1f", degrees, speed));
        double turnApproximation = 2;
        double startingHeading = getTotalDegreesTurned();
        double endHeading = startingHeading - degrees + turnApproximation;

        double degreesToGo = -degrees;

        while (shouldRobotKeepRunning() && degreesToGo < 0) {
            // Goal - Current
            degreesToGo = endHeading - getTotalDegreesTurned();

            opMode.setStatus(String.format("%.1f degrees to go. ", degreesToGo));
            if (degreesToGo > -20) {
                spin(0.2);
            } else {
                spin(speed);
            }
        }
        opMode.setStatus("Right turn is done");
        stop();
    }

    public void turnLeft(double degrees, double speed) {
        opMode.setOperation(String.format("TurnLeft(d=%.1f, s=%.1f", degrees, speed));
        double turnApproximation = 2;
        double startingHeading = getTotalDegreesTurned();
        double endHeading = startingHeading + degrees - turnApproximation;

        double degreesToGo = degrees;

        while (shouldRobotKeepRunning() && degreesToGo > 0) {
            // Goal - Current
            degreesToGo = endHeading - getTotalDegreesTurned();

            opMode.setStatus(String.format("%.1f degrees to go. ", degreesToGo));
            if (degreesToGo < 20) {
                spin(0.2);
            } else {
                spin(speed);
            }
        }
        opMode.setStatus("Turn left is done");
        stop();
    }

    // Calibrations
    public void calibrateHook() {
        opMode.setOperation("Calibrating hook");
        int change;
        int oldValue = hookMotor.getCurrentPosition();
        setHookPower(-0.15);

        while (shouldRobotKeepRunning()) {
            opMode.teamIdle();
            opMode.sleep(250);
            int newValue = hookMotor.getCurrentPosition();
            change = newValue - oldValue;
            oldValue = newValue;

            opMode.setStatus(String.format("Position=%d, change=%d", newValue, change));
            // If it isn't getting more negative
            if (change > -5) {
                setHookPower(0);
                break;
            }
        }
        hook0 = hookMotor.getCurrentPosition() + 500;
        hookCalibrated = true;
        opMode.setStatus("Hook calibration done");
    }

    public void calibrateArmSwing() {
        opMode.setOperation("Calibrating arm swing");
        int change;
        int oldValue = swingMotor.getCurrentPosition();
        setSwingArm(-0.15);

        while (shouldRobotKeepRunning()) {
            opMode.teamIdle();
            opMode.sleep(250);
            int newValue = swingMotor.getCurrentPosition();
            change = newValue - oldValue;
            oldValue = newValue;

            opMode.setStatus(String.format("Position=%d, change=%d", newValue, change));
            // If it isn't getting more negative
            if (change > -5) {
                setHookPower(0);
                break;
            }
        }

        swing0 = swingMotor.getCurrentPosition() + 500;
        armSwingCalibrated = true;
        opMode.setStatus("Arm swing calibration done");
    }

    public void calibrateArmExtension() {
        opMode.setOperation("Calibrating arm extension");
        int change;
        int oldValue = armExtensionMotor.getCurrentPosition();
        opMode.setStatus(String.format("Initial Position=%d", oldValue));

        setArmExtensionPower(-0.1);

        while (shouldRobotKeepRunning()) {
            opMode.sleep(500);
            int newValue = armExtensionMotor.getCurrentPosition();
            change = newValue - oldValue;
            oldValue = newValue;

            opMode.setStatus(String.format("Position=%d, change=%d", newValue, change));
            // If it isn't getting more negative
            if (change > -5) {
                setArmExtensionPower(0);
                break;
            }
        }

        extension0 = armExtensionMotor.getCurrentPosition() + 500;
        armExtensionCalibrated = true;
        opMode.setStatus("Arm extension calibration done");
    }

    public void calibrateEverything()
    {
        calibrateHook();
        calibrateArmExtension();
        calibrateArmSwing();
    }
}