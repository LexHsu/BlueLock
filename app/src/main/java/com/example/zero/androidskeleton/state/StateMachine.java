package com.example.zero.androidskeleton.state;


import com.example.zero.androidskeleton.log.Log;

/**
 * Created by zero on 5/14/16.
 */
public class StateMachine {

    private static final String TAG = "StateMachine";

    private Context context = new Context(this);
    private State state = null;

    protected synchronized void init(State initState) {
        if (state != null) {
            throw new IllegalStateException("initial state already exists");
        }

        this.state = initState;
    }

    public synchronized void handle(int event, int arg, Object o) {
        Log.i(TAG, "state=" + state + ", event=" + event);
        state.handle(context, event, arg, o);
    }

    void setState(State newState) {
        this.state = newState;
    }

    public State getState() {
        return this.state;
    }

    Context getContext() {
        return context;
    }
}
