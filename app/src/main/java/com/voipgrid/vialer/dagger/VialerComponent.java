package com.voipgrid.vialer.dagger;

import com.voipgrid.vialer.CallActivity;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.calling.AbstractCallActivity;
import com.voipgrid.vialer.calling.IncomingCallActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {VialerModule.class})
public interface VialerComponent {
    void inject(VialerApplication app);

    void inject(AbstractCallActivity abstractCallActivity);

    void inject(CallActivity callActivity);

    void inject(IncomingCallActivity incomingCallActivity);
}
