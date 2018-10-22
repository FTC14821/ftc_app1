package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Func;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.robotcore.external.navigation.Velocity;

import java.util.Locale;

public class TeamImu  {
    // Gyro
    BNO055IMU imu;

    // Needed for gyro telemetry
    Orientation angles;
    Acceleration gravity;
    Position position;
    Velocity velocity;
    Acceleration linearAcceleration;
    Acceleration acceleration;

    BNO055IMU.Parameters parameters;
    Acceleration accelrationDrift;


    public TeamImu initialize(HardwareMap hardwareMap, Telemetry telemetry) {
        this.parameters = parameters;
        // Retrieve and initialize the IMU. We expect the IMU to be attached to an I2C port
        // on a Core Device Interface Module, configured to be a sensor of type "AdaFruit IMU",
        // and named "imu".
        imu = hardwareMap.get(BNO055IMU.class, "imu");

        // Set up the parameters with which we will use our IMU. Note that integration
        // algorithm here just reports accelerations to the logcat log; it doesn't actually
        // provide positional information.
        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.angleUnit           = BNO055IMU.AngleUnit.DEGREES;
        parameters.accelUnit           = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.calibrationDataFile = "BNO055IMUCalibration.json"; // see the calibration sample opmode
        parameters.loggingEnabled      = true;
        parameters.loggingTag          = "IMU";


        imu.initialize(parameters);

        // Set up our telemetry dashboard
        setupTelemetry(telemetry);

        return this;
    }

    public void start() {
    }


    /**
     * Adjust for phantom acceleration
     */
    public void calibrateWhileRobotIsNotMoving() {
        Acceleration totalAccelerationsMeasured = null;
        for (int i = 0; i < 10; i++) {
            Acceleration anAccelerationMeasurement = imu.getLinearAcceleration();
            if (totalAccelerationsMeasured == null) {
                totalAccelerationsMeasured = anAccelerationMeasurement;
            } else {
                totalAccelerationsMeasured.xAccel += anAccelerationMeasurement.xAccel;
                totalAccelerationsMeasured.yAccel += anAccelerationMeasurement.yAccel;
                totalAccelerationsMeasured.zAccel += anAccelerationMeasurement.zAccel;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        accelrationDrift = new Acceleration(
                totalAccelerationsMeasured.unit,
                totalAccelerationsMeasured.xAccel / 10,
                totalAccelerationsMeasured.yAccel / 10,
                totalAccelerationsMeasured.zAccel / 10,
                totalAccelerationsMeasured.acquisitionTime);
    }

    void setupTelemetry(Telemetry telemetry) {
        // At the beginning of each telemetry update, grab a bunch of data
        // from the IMU that we will then display in separate lines.
        telemetry.addAction(new Runnable() {
            @Override
            public void run() {
                // Acquiring the angles is relatively expensive; we don't want
                // to do that in each of the three items that need that info, as that's
                // three times the necessary expense.
                angles = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);
                gravity = imu.getGravity();
                position = imu.getPosition();
                linearAcceleration = imu.getLinearAcceleration();
                acceleration = imu.getAcceleration();
                velocity = imu.getVelocity();
            }
        });

        telemetry.addLine()
                .addData("heading", new Func<String>() {
                    @Override
                    public String value() {
                        return formatAngle(angles.angleUnit, angles.firstAngle);
                    }
                })
                .addData("roll", new Func<String>() {
                    @Override
                    public String value() {
                        return formatAngle(angles.angleUnit, angles.secondAngle);
                    }
                })
                .addData("pitch", new Func<String>() {
                    @Override
                    public String value() {
                        return formatAngle(angles.angleUnit, angles.thirdAngle);
                    }
                });
        telemetry.addLine()
                .addData("P: ", new Func<String>() {
                    @Override
                    public String value() {
                        return String.format("(%.1f, %.1f, %.1f)", position.x, position.y, position.z);
                    }
                })
                .addData("V: ", new Func<String>() {
                    @Override
                    public String value() {
                        return String.format("(%.1f, %.1f, %.1f)", velocity.xVeloc, velocity.yVeloc, velocity.zVeloc);
                    }
                })
                .addData("A: ", new Func<String>() {
                    @Override
                    public String value() {
                        return String.format("(%.1f, %.1f, %.1f)", acceleration.xAccel, acceleration.yAccel, acceleration.zAccel);
                    }
                })
                .addData("LinA: ", new Func<String>() {
                    @Override
                    public String value() {
                        return String.format("(%.1f, %.1f, %.1f)", linearAcceleration.xAccel, linearAcceleration.yAccel, linearAcceleration.zAccel);
                    }
                })
        ;

    }

    public float getHeading() {
        return imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES).firstAngle;
    }


    //----------------------------------------------------------------------------------------------
    // Formatting
    //----------------------------------------------------------------------------------------------

    String formatAngle(AngleUnit angleUnit, double angle) {
        return formatDegrees(AngleUnit.DEGREES.fromUnit(angleUnit, angle));
    }

    String formatDegrees(double degrees) {
        return String.format(Locale.getDefault(), "%.1f", AngleUnit.DEGREES.normalize(degrees));
    }

}