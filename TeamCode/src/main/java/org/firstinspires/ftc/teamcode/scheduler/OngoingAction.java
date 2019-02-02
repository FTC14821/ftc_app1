package org.firstinspires.ftc.teamcode.scheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.firstinspires.ftc.teamcode.scheduler.Utils.*;

public abstract class OngoingAction extends Action{
    // Managed by scheduler
        int numberOfLoops=0;
        long lastLoopDuration_ns;
        long lastLoopStart_ns=0;

    public OngoingAction(String label)
    {
        this(label, null);
    }

    public OngoingAction(String label, String descriptionFormat, Object...descriptionArgs){
        super(label, descriptionFormat, descriptionArgs);
    }

    // Only called by scheduler
    protected void loop()
    {
    }

}