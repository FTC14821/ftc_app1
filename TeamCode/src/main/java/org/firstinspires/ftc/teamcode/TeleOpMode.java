package org.firstinspires.ftc.teamcode;

import org.firstinspires.ftc.robotcore.external.Func;

abstract class TeleOpMode extends BaseLinearOpMode {

    @Override
    public void teamInit() {
        telemetry.addLine("GP1:")
                .addData("B", new Func<String>() {
                    @Override public String value() {
                        return String.format("ABXY=%s%s%s%s", bool2tf(gamepad1.a), bool2tf(gamepad1.b), bool2tf(gamepad1.x), bool2tf(gamepad1.y));
                    }})
                .addData("D", new Func<String>() {
                    @Override public String value() {
                        return String.format("LUDR=%s%s%s%s", bool2tf(gamepad1.dpad_left), bool2tf(gamepad1.dpad_up), bool2tf(gamepad1.dpad_down), bool2tf(gamepad1.dpad_right));
                    }})
                .addData("LJS", new Func<String>() {
                    @Override public String value() {
                        return String.format("(%.2f, %.2f)", gamepad1.left_stick_x, gamepad1.left_stick_y);
                    }})
                .addData("LTrig/Bump", new Func<String>() {
                    @Override public String value() {
                        return String.format("%.2f/%s", gamepad1.left_trigger, bool2tf(gamepad1.left_bumper));
                    }})
                ;

        telemetry.addLine("GP2:")
                .addData("B", new Func<String>() {
                    @Override public String value() {
                        return String.format("ABXY=%s%s%s%s", bool2tf(gamepad2.a), bool2tf(gamepad2.b), bool2tf(gamepad2.x), bool2tf(gamepad2.y));
                    }})
                .addData("D", new Func<String>() {
                    @Override public String value() {
                        return String.format("LUDR=%s%s%s%s", bool2tf(gamepad2.dpad_left), bool2tf(gamepad2.dpad_up), bool2tf(gamepad2.dpad_down), bool2tf(gamepad2.dpad_right));
                    }})
                .addData("LJS", new Func<String>() {
                    @Override public String value() {
                        return String.format("(%.2f, %.2f)", gamepad2.left_stick_x, gamepad2.left_stick_y);
                    }})
                .addData("LTrig/Bump", new Func<String>() {
                    @Override public String value() {
                        return String.format("%.2f/%s", gamepad2.left_trigger, bool2tf(gamepad2.left_bumper));
                    }})
        ;
    }

    private String bool2tf(boolean b) {
        return b ? "t" : "f";
    }


    public double getArmExtensionSlowDown()
    {
        // Slowdown will be 1-->7
        double armSlowDown = 1 + (gamepad2.right_bumper ? 6 : 0);
        return armSlowDown;
    }

    public double getArmSwingSlowDown()
    {
        // Slowdown will be 1-->7
        double armSwingSlowDown = 1 + (gamepad2.left_bumper ? 1 : 0);
        return armSwingSlowDown;
    }

    public double getDrivingSlowDown()
    {
        // Slowdown will be 1-->5
        double drivingSlowDown = 1 + gamepad1.left_trigger * 4;
        return drivingSlowDown;
    }

    public double getHookSlowDown()
    {
        // Slowdown will be 1-->5
        double hookSlowDown = 1 + (gamepad2.left_bumper ? 9 : 0);
        return hookSlowDown;
    }

    // First TeleOp thing that happens after play button is pressed
    public void teleOpStart()
    {
    }

    // Convert LinearOpMode into OpMode
    @Override
    void teamRun()
    {
        teleOpStart();

        while ( shouldOpModeKeepRunning() )
        {
            teleOpLoop();
        }
    }


    // Called over and over until stop button is pressed
    public void teleOpLoop() {
        robot.setDrivingSlowdown(getDrivingSlowDown());
        robot.setArmSlowdown(getArmExtensionSlowDown());
        robot.setHookSlowdown(getHookSlowDown());


        if(gamepad1.back)
        {
            robot.reverseRobotOrientation();

            //Wait until button is released before switching front and back
            while (gamepad1.back)
            {
                sleep(1);
            }
        }

        if(gamepad1.right_bumper)
        {
            while(gamepad1.right_bumper)
            {

            }
            if(robot.mineralPlowServo.getPosition() == 0)
            {
                robot.mineralPlowServo.setPosition(0.85);
            }
            else if(robot.mineralPlowServo.getPosition() > 0.8)
            {
                robot.mineralPlowServo.setPosition(0);
            }
        }

        robot.setArmExtensionPower(-gamepad2.left_stick_y / getArmExtensionSlowDown());
        robot.setGrabberServoPosition(-gamepad2.right_stick_x);
        robot.setSwingArm(-gamepad2.right_stick_y);
        if(gamepad2.dpad_up)
        {
            robot.setHookPower(1);
        }
        else if(gamepad2.dpad_down)
        {
            robot.setHookPower(-1);
        }
        else
        {
            robot.setHookPower(0);
        }
    }
}
