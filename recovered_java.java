/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.app.Application
 *  android.app.NotificationManager
 *  android.content.ComponentName
 *  android.content.Context
 *  android.content.Intent
 *  android.content.SharedPreferences
 *  android.location.Location
 *  android.location.LocationManager
 *  android.net.ConnectivityManager
 *  android.net.Network
 *  android.net.NetworkCapabilities
 *  android.os.Build$VERSION
 *  android.provider.Settings
 *  android.util.Log
 *  androidx.compose.runtime.internal.StabilityInferred
 *  androidx.core.content.ContextCompat
 *  androidx.lifecycle.AndroidViewModel
 *  androidx.lifecycle.ViewModel
 *  androidx.lifecycle.ViewModelKt
 *  dagger.hilt.android.lifecycle.HiltViewModel
 *  javax.inject.Inject
 *  kotlin.KotlinNothingValueException
 *  kotlin.Metadata
 *  kotlin.NoWhenBranchMatchedException
 *  kotlin.Pair
 *  kotlin.Result
 *  kotlin.ResultKt
 *  kotlin.Unit
 *  kotlin.collections.CollectionsKt
 *  kotlin.coroutines.Continuation
 *  kotlin.coroutines.intrinsics.IntrinsicsKt
 *  kotlin.coroutines.jvm.internal.Boxing
 *  kotlin.coroutines.jvm.internal.SpillingKt
 *  kotlin.jvm.functions.Function0
 *  kotlin.jvm.functions.Function1
 *  kotlin.jvm.functions.Function2
 *  kotlin.jvm.internal.DefaultConstructorMarker
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.text.StringsKt
 *  kotlinx.coroutines.BuildersKt
 *  kotlinx.coroutines.CoroutineScope
 *  kotlinx.coroutines.DelayKt
 *  kotlinx.coroutines.Job
 *  kotlinx.coroutines.flow.FlowCollector
 *  kotlinx.coroutines.flow.FlowKt
 *  kotlinx.coroutines.flow.MutableSharedFlow
 *  kotlinx.coroutines.flow.MutableStateFlow
 *  kotlinx.coroutines.flow.SharedFlow
 *  kotlinx.coroutines.flow.SharedFlowKt
 *  kotlinx.coroutines.flow.SharingStarted
 *  kotlinx.coroutines.flow.SharingStarted$Companion
 *  kotlinx.coroutines.flow.StateFlow
 *  kotlinx.coroutines.flow.StateFlowKt
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.example.ui;

import android.app.Application;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import androidx.compose.runtime.internal.StabilityInferred;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelKt;
import com.example.ble.HardwareGpsLocation;
import com.example.ble.HardwareGpsState;
import com.example.ble.MotionState;
import com.example.ble.Mpu6050Reading;
import com.example.ble.MpuHardwareState;
import com.example.data.FallDatabase;
import com.example.data.SettingsDataStore;
import com.example.model.AIAnalysisModel;
import com.example.model.AISensorReading;
import com.example.model.AITimelineEvent;
import com.example.model.AiAnalysisResult;
import com.example.model.Alert;
import com.example.model.DeveloperLog;
import com.example.model.Device;
import com.example.model.EmergencyContact;
import com.example.model.EmergencyModel;
import com.example.model.EmergencySession;
import com.example.model.FallEvent;
import com.example.model.HistoryModel;
import com.example.model.NotificationItem;
import com.example.model.NotificationModel;
import com.example.model.PermissionsState;
import com.example.model.SensorReading;
import com.example.model.SosWorkflowState;
import com.example.model.TrustedPlace;
import com.example.model.User;
import com.example.model.UserLocation;
import com.example.repository.FallRepository;
import com.example.service.AIProvider;
import com.example.service.AIService;
import com.example.service.AiAnalysisService;
import com.example.service.AlarmVibratorService;
import com.example.service.AnalyticsService;
import com.example.service.AuthService;
import com.example.service.AuthState;
import com.example.service.DatabaseService;
import com.example.service.DeviceService;
import com.example.service.EmergencyProvider;
import com.example.service.EmergencyService;
import com.example.service.FallDetectionService;
import com.example.service.HistoryProvider;
import com.example.service.HistoryService;
import com.example.service.LocationService;
import com.example.service.NotificationProvider;
import com.example.service.NotificationService;
import com.example.service.SafetyTimerService;
import com.example.service.SecurityService;
import com.example.service.TrustedPlacesService;
import com.example.service.VoiceActivationLog;
import com.example.service.VoiceCommand;
import com.example.service.VoiceSosForegroundService;
import com.example.service.VoiceSosService;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.inject.Inject;
import kotlin.KotlinNothingValueException;
import kotlin.Metadata;
import kotlin.NoWhenBranchMatchedException;
import kotlin.Pair;
import kotlin.Result;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.coroutines.jvm.internal.Boxing;
import kotlin.coroutines.jvm.internal.SpillingKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.DelayKt;
import kotlinx.coroutines.Job;
import kotlinx.coroutines.flow.FlowCollector;
import kotlinx.coroutines.flow.FlowKt;
import kotlinx.coroutines.flow.MutableSharedFlow;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.SharedFlow;
import kotlinx.coroutines.flow.SharedFlowKt;
import kotlinx.coroutines.flow.SharingStarted;
import kotlinx.coroutines.flow.StateFlow;
import kotlinx.coroutines.flow.StateFlowKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={2, 2, 0}, k=1, xi=48, d1={"\u0000\u00da\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b1\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u000e\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\u0007\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0006\n\u0002\bD\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u000f\n\u0002\u0018\u0002\n\u0002\b0\n\u0002\u0018\u0002\n\u0002\b(\n\u0002\u0018\u0002\n\u0002\b\u000f\n\u0002\u0018\u0002\n\u0002\b\u000f\b\u0007\u0018\u00002\u00020\u0001:\u0002\u00cc\u0003B\u00c9\u0001\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u0012\u0006\u0010\f\u001a\u00020\r\u0012\u0006\u0010\u000e\u001a\u00020\u000f\u0012\u0006\u0010\u0010\u001a\u00020\u0011\u0012\u0006\u0010\u0012\u001a\u00020\u0013\u0012\u0006\u0010\u0014\u001a\u00020\u0015\u0012\u0006\u0010\u0016\u001a\u00020\u0017\u0012\u0006\u0010\u0018\u001a\u00020\u0019\u0012\u0006\u0010\u001a\u001a\u00020\u001b\u0012\u0006\u0010\u001c\u001a\u00020\u001d\u0012\u0006\u0010\u001e\u001a\u00020\u001f\u0012\u0006\u0010 \u001a\u00020!\u0012\u0006\u0010\"\u001a\u00020#\u0012\u0006\u0010$\u001a\u00020%\u0012\u0006\u0010&\u001a\u00020'\u0012\u0006\u0010(\u001a\u00020)\u0012\u0006\u0010*\u001a\u00020+\u0012\u0006\u0010,\u001a\u00020-\u0012\u0006\u0010.\u001a\u00020/\u0012\u0006\u00100\u001a\u000201\u00a2\u0006\u0004\b2\u00103J\u000e\u0010g\u001a\u00020h2\u0006\u0010i\u001a\u00020dJ\u000e\u0010n\u001a\u00020h2\u0006\u0010i\u001a\u00020dJ\u000e\u0010o\u001a\u00020h2\u0006\u0010i\u001a\u00020dJ\u0010\u0010\u00f6\u0001\u001a\u00020h2\u0007\u0010\u00f7\u0001\u001a\u00020uJ\u0010\u0010\u00fb\u0001\u001a\u00020h2\u0007\u0010\u00fc\u0001\u001a\u00020uJ\u0007\u0010\u00fd\u0001\u001a\u00020hJ\u000f\u0010\u0081\u0002\u001a\u00020h2\u0006\u0010i\u001a\u00020dJ\u0010\u0010\u0082\u0002\u001a\u00020h2\u0007\u0010\u0083\u0002\u001a\u00020uJ\u000f\u0010\u0084\u0002\u001a\u00020h2\u0006\u0010i\u001a\u00020dJ\u000f\u0010\u0088\u0002\u001a\u00020h2\u0006\u0010i\u001a\u00020dJ\u000f\u0010\u008c\u0002\u001a\u00020h2\u0006\u0010i\u001a\u00020dJ\u0010\u0010\u0090\u0002\u001a\u00020h2\u0007\u0010\u0091\u0002\u001a\u00020uJ\u000f\u0010\u0095\u0002\u001a\u00020h2\u0006\u0010i\u001a\u00020dJ\u000f\u0010\u0099\u0002\u001a\u00020h2\u0006\u0010i\u001a\u00020dJ\u000f\u0010\u009d\u0002\u001a\u00020h2\u0006\u0010i\u001a\u00020dJ\u0018\u0010\u00a4\u0002\u001a\u00020h2\u0007\u0010\u00a5\u0002\u001a\u00020u2\u0006\u0010i\u001a\u00020dJ\u0010\u0010\u00a9\u0002\u001a\u00020h2\u0007\u0010\u00a5\u0002\u001a\u00020uJ\u0007\u0010\u00af\u0002\u001a\u00020hJ\u0007\u0010\u00b0\u0002\u001a\u00020hJ/\u0010\u00b1\u0002\u001a\u00020h2\u0007\u0010\u00b2\u0002\u001a\u00020u2\u0007\u0010\u00b3\u0002\u001a\u00020u2\u0014\u0010\u00b4\u0002\u001a\u000f\u0012\u0004\u0012\u00020d\u0012\u0004\u0012\u00020h0\u00b5\u0002J\u0007\u0010\u00b6\u0002\u001a\u00020hJ\u0011\u0010\u00c2\u0002\u001a\u00020h2\b\u0010\u00c3\u0002\u001a\u00030\u00c4\u0002J\u0019\u0010\u00c7\u0002\u001a\u00020h2\u0007\u0010\u00c8\u0002\u001a\u00020u2\u0007\u0010\u00c9\u0002\u001a\u00020uJF\u0010\u00ca\u0002\u001a\u00020h2\u0007\u0010\u00cb\u0002\u001a\u00020u2\u0007\u0010\u00c8\u0002\u001a\u00020u2\u0007\u0010\u00cc\u0002\u001a\u00020u2\u0007\u0010\u00cd\u0002\u001a\u00020u2\u0007\u0010\u00ce\u0002\u001a\u00020u2\u0007\u0010\u00cf\u0002\u001a\u00020u2\u0007\u0010\u00c9\u0002\u001a\u00020uJ\u0010\u0010\u00d0\u0002\u001a\u00020h2\u0007\u0010\u00c8\u0002\u001a\u00020uJ\u0007\u0010\u00d1\u0002\u001a\u00020hJ\u0011\u0010\u00d2\u0002\u001a\u00020h2\b\u0010\u00d3\u0002\u001a\u00030\u00d4\u0002J\u0007\u0010\u00d5\u0002\u001a\u00020dJ \u0010\u00d6\u0002\u001a\u0005\u0018\u00010\u00d3\u00012\b\u0010\u00d7\u0002\u001a\u00030\u00f0\u00012\b\u0010\u00d8\u0002\u001a\u00030\u00f0\u0001H\u0002Jw\u0010\u00d9\u0002\u001a\u00030\u00cc\u00012\u0007\u0010\u00da\u0002\u001a\u00020u2\u0007\u0010\u00db\u0002\u001a\u00020u2\f\b\u0002\u0010\u00d7\u0002\u001a\u0005\u0018\u00010\u00f0\u00012\f\b\u0002\u0010\u00d8\u0002\u001a\u0005\u0018\u00010\u00f0\u00012\f\b\u0002\u0010\u00dc\u0002\u001a\u0005\u0018\u00010\u00aa\u00012\f\b\u0002\u0010\u00dd\u0002\u001a\u0005\u0018\u00010\u00f0\u00012\f\b\u0002\u0010\u00de\u0002\u001a\u0005\u0018\u00010\u00aa\u00012\f\b\u0002\u0010\u00df\u0002\u001a\u0005\u0018\u00010\u00aa\u0001H\u0082@\u00a2\u0006\u0003\u0010\u00e0\u0002J\u0007\u0010\u00e1\u0002\u001a\u00020hJ\u001f\u0010\u00e2\u0002\u001a\u00020h2\n\b\u0002\u0010\u00d7\u0002\u001a\u00030\u00f0\u00012\n\b\u0002\u0010\u00d8\u0002\u001a\u00030\u00f0\u0001J\u0007\u0010\u00e3\u0002\u001a\u00020hJ\u0019\u0010\u00e4\u0002\u001a\u00020h2\u0007\u0010\u00e5\u0002\u001a\u00020u2\u0007\u0010\u00e6\u0002\u001a\u00020{J\u001a\u0010\u00e7\u0002\u001a\u00020h2\b\u0010\u00e8\u0002\u001a\u00030\u00b7\u00012\u0007\u0010\u00e6\u0002\u001a\u00020{J\u0011\u0010\u00e9\u0002\u001a\u00020h2\b\u0010\u00c3\u0002\u001a\u00030\u00c4\u0002J\u0007\u0010\u00ea\u0002\u001a\u00020hJ\u0007\u0010\u00eb\u0002\u001a\u00020hJ\u0019\u0010\u00ec\u0002\u001a\u00020h2\u0007\u0010\u00ed\u0002\u001a\u00020u2\u0007\u0010\u00ee\u0002\u001a\u00020uJP\u0010\u00ef\u0002\u001a\u00020h2\u0007\u0010\u00cb\u0002\u001a\u00020u2\u0007\u0010\u00f0\u0002\u001a\u00020u2\t\b\u0002\u0010\u00db\u0002\u001a\u00020u2\t\b\u0002\u0010\u00f1\u0002\u001a\u00020u2\t\b\u0002\u0010\u00f2\u0002\u001a\u00020{2\t\b\u0002\u0010\u00f3\u0002\u001a\u00020{2\t\b\u0002\u0010\u00f4\u0002\u001a\u00020uJ\u0019\u0010\u00f5\u0002\u001a\u00020h2\u0007\u0010\u00db\u0002\u001a\u00020u2\u0007\u0010\u00f6\u0002\u001a\u00020uJ\u0010\u0010\u00f7\u0002\u001a\u00020h2\u0007\u0010\u00db\u0002\u001a\u00020uJ\u0011\u0010\u00f8\u0002\u001a\u00020h2\b\u0010\u00f9\u0002\u001a\u00030\u00e9\u0001J\u0010\u0010\u00fa\u0002\u001a\u00020h2\u0007\u0010\u00fb\u0002\u001a\u00020uJ\u0007\u0010\u00fc\u0002\u001a\u00020hJ\u0007\u0010\u00fd\u0002\u001a\u00020hJ-\u0010\u00fe\u0002\u001a\u00020h2\u0007\u0010\u00cb\u0002\u001a\u00020u2\b\u0010\u00d7\u0002\u001a\u00030\u00f0\u00012\b\u0010\u00d8\u0002\u001a\u00030\u00f0\u00012\u0007\u0010\u00ff\u0002\u001a\u00020uJ\u0010\u0010\u0080\u0003\u001a\u00020h2\u0007\u0010\u0081\u0003\u001a\u00020uJ!\u0010\u0082\u0003\u001a\u0013\u0012\u0005\u0012\u00030\u00f0\u0001\u0012\u0005\u0012\u00030\u00f0\u0001\u0018\u00010\u00ef\u00012\u0007\u0010\u0083\u0003\u001a\u00020uJ\u0013\u0010\u0084\u0003\u001a\u0005\u0018\u00010\u0085\u0003H\u0086@\u00a2\u0006\u0003\u0010\u0086\u0003J\u0011\u0010\u0087\u0003\u001a\u00020h2\b\u0010\u0088\u0003\u001a\u00030\u00d3\u0001J\u0011\u0010\u0089\u0003\u001a\u00020h2\b\u0010\u0088\u0003\u001a\u00030\u00d3\u0001J\u0010\u0010\u008a\u0003\u001a\u00020h2\u0007\u0010\u008b\u0003\u001a\u00020uJ\u0019\u0010\u008c\u0003\u001a\u00020h2\u0007\u0010\u00f7\u0001\u001a\u00020u2\u0007\u0010\u008d\u0003\u001a\u00020dJ\u0007\u0010\u008e\u0003\u001a\u00020hJ\u0010\u0010\u008f\u0003\u001a\u00020h2\u0007\u0010\u0083\u0003\u001a\u00020uJ\u0012\u0010\u0090\u0003\u001a\u00020h2\t\b\u0002\u0010\u0091\u0003\u001a\u00020uJ\u0007\u0010\u0092\u0003\u001a\u00020hJ\u0010\u0010\u0093\u0003\u001a\u00020h2\u0007\u0010\u0094\u0003\u001a\u00020uJ\u0007\u0010\u0095\u0003\u001a\u00020hJ\u0007\u0010\u0096\u0003\u001a\u00020hJ&\u0010\u0097\u0003\u001a\u00020h2\u0007\u0010\u00a5\u0002\u001a\u00020u2\u0014\u0010\u00b4\u0002\u001a\u000f\u0012\u0004\u0012\u00020d\u0012\u0004\u0012\u00020h0\u00b5\u0002J\u0012\u0010\u0098\u0003\u001a\u00020h2\t\b\u0002\u0010\u00ee\u0002\u001a\u00020uJ\u0010\u0010\u0099\u0003\u001a\u00020h2\u0007\u0010\u0081\u0003\u001a\u00020uJ\u0007\u0010\u009a\u0003\u001a\u00020uJ\u0007\u0010\u009b\u0003\u001a\u00020uJ\u0010\u0010\u009c\u0003\u001a\u00020h2\u0007\u0010\u0081\u0003\u001a\u00020uJ\u0010\u0010\u009d\u0003\u001a\u00020h2\u0007\u0010\u0081\u0003\u001a\u00020uJ\u0007\u0010\u009e\u0003\u001a\u00020hJ\u0007\u0010\u009f\u0003\u001a\u00020hJ\u0010\u0010\u00a0\u0003\u001a\u00020h2\u0007\u0010\u0081\u0003\u001a\u00020uJ\u0010\u0010\u00a1\u0003\u001a\u00020h2\u0007\u0010\u0081\u0003\u001a\u00020uJ\u0007\u0010\u00a2\u0003\u001a\u00020hJ\u0010\u0010\u00a3\u0003\u001a\u00020h2\u0007\u0010\u00db\u0002\u001a\u00020uJ\u0010\u0010\u00a4\u0003\u001a\u00020h2\u0007\u0010\u00db\u0002\u001a\u00020uJ\u0007\u0010\u00a5\u0003\u001a\u00020hJ\u0010\u0010\u00a6\u0003\u001a\u00020h2\u0007\u0010\u00a7\u0003\u001a\u00020dJ\u0010\u0010\u00a8\u0003\u001a\u00020h2\u0007\u0010\u00a9\u0003\u001a\u00020uJ\u0007\u0010\u00aa\u0003\u001a\u00020hJI\u0010\u00ab\u0003\u001a\u00020h2\u0007\u0010\u00cb\u0002\u001a\u00020u2\u0007\u0010\u00f0\u0002\u001a\u00020u2\u0007\u0010\u00ac\u0003\u001a\u00020u2\u0007\u0010\u00f1\u0002\u001a\u00020u2\u001c\u0010\u00ad\u0003\u001a\u0017\u0012\f\u0012\n\u0012\u0005\u0012\u00030\u00e6\u00010\u00ae\u0003\u0012\u0004\u0012\u00020h0\u00b5\u0002J\u0007\u0010\u00af\u0003\u001a\u00020hJ\u0010\u0010\u00b2\u0003\u001a\u00020h2\u0007\u0010\u00b3\u0003\u001a\u00020dJ\u0010\u0010\u00b4\u0003\u001a\u00020h2\u0007\u0010\u00b5\u0003\u001a\u00020dJ\u000f\u0010\u00b9\u0003\u001a\u00020h2\u0006\u0010i\u001a\u00020dJ\u000f\u0010\u00ba\u0003\u001a\u00020h2\u0006\u0010i\u001a\u00020dJ\u0007\u0010\u00bb\u0003\u001a\u00020hJ\u0007\u0010\u00bc\u0003\u001a\u00020hJ\u0019\u0010\u00c0\u0003\u001a\u00020h2\u0007\u0010\u00c1\u0003\u001a\u00020u2\u0007\u0010\u00c2\u0003\u001a\u00020uJ\u0007\u0010\u00c3\u0003\u001a\u00020hJ\u0007\u0010\u00c4\u0003\u001a\u00020hJ\u001b\u0010\u00c5\u0003\u001a\u00020h2\b\u0010\u00d7\u0002\u001a\u00030\u00f0\u00012\b\u0010\u00d8\u0002\u001a\u00030\u00f0\u0001J\u0010\u0010\u00c6\u0003\u001a\u00020h2\u0007\u0010\u00db\u0002\u001a\u00020uJ\u0010\u0010\u00c7\u0003\u001a\u00020h2\u0007\u0010\u00db\u0002\u001a\u00020uJ\t\u0010\u00c8\u0003\u001a\u00020hH\u0014J\u0007\u0010\u00c9\u0003\u001a\u00020hJ\u0007\u0010\u00ca\u0003\u001a\u00020hJ\u0010\u0010\u00cb\u0003\u001a\u00020h2\u0007\u0010\u00db\u0002\u001a\u00020uR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b4\u00105R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b6\u00107R\u0011\u0010\b\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b8\u00109R\u0011\u0010\n\u001a\u00020\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b:\u0010;R\u0011\u0010\f\u001a\u00020\r\u00a2\u0006\b\n\u0000\u001a\u0004\b<\u0010=R\u0011\u0010\u000e\u001a\u00020\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b>\u0010?R\u0011\u0010\u0010\u001a\u00020\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b@\u0010AR\u0011\u0010\u0012\u001a\u00020\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\bB\u0010CR\u0011\u0010\u0014\u001a\u00020\u0015\u00a2\u0006\b\n\u0000\u001a\u0004\bD\u0010ER\u0011\u0010\u0016\u001a\u00020\u0017\u00a2\u0006\b\n\u0000\u001a\u0004\bF\u0010GR\u0011\u0010\u0018\u001a\u00020\u0019\u00a2\u0006\b\n\u0000\u001a\u0004\bH\u0010IR\u0011\u0010\u001a\u001a\u00020\u001b\u00a2\u0006\b\n\u0000\u001a\u0004\bJ\u0010KR\u0011\u0010\u001c\u001a\u00020\u001d\u00a2\u0006\b\n\u0000\u001a\u0004\bL\u0010MR\u0011\u0010\u001e\u001a\u00020\u001f\u00a2\u0006\b\n\u0000\u001a\u0004\bN\u0010OR\u0011\u0010 \u001a\u00020!\u00a2\u0006\b\n\u0000\u001a\u0004\bP\u0010QR\u0011\u0010\"\u001a\u00020#\u00a2\u0006\b\n\u0000\u001a\u0004\bR\u0010SR\u0011\u0010$\u001a\u00020%\u00a2\u0006\b\n\u0000\u001a\u0004\bT\u0010UR\u0011\u0010&\u001a\u00020'\u00a2\u0006\b\n\u0000\u001a\u0004\bV\u0010WR\u0011\u0010(\u001a\u00020)\u00a2\u0006\b\n\u0000\u001a\u0004\bX\u0010YR\u0011\u0010*\u001a\u00020+\u00a2\u0006\b\n\u0000\u001a\u0004\bZ\u0010[R\u0011\u0010,\u001a\u00020-\u00a2\u0006\b\n\u0000\u001a\u0004\b\\\u0010]R\u0011\u0010.\u001a\u00020/\u00a2\u0006\b\n\u0000\u001a\u0004\b^\u0010_R\u0011\u00100\u001a\u000201\u00a2\u0006\b\n\u0000\u001a\u0004\b`\u0010aR\u0017\u0010b\u001a\b\u0012\u0004\u0012\u00020d0c\u00a2\u0006\b\n\u0000\u001a\u0004\be\u0010fR\u0017\u0010j\u001a\b\u0012\u0004\u0012\u00020d0c\u00a2\u0006\b\n\u0000\u001a\u0004\bk\u0010fR\u0017\u0010l\u001a\b\u0012\u0004\u0012\u00020d0c\u00a2\u0006\b\n\u0000\u001a\u0004\bm\u0010fR\u0014\u0010p\u001a\b\u0012\u0004\u0012\u00020d0qX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010r\u001a\b\u0012\u0004\u0012\u00020d0c\u00a2\u0006\b\n\u0000\u001a\u0004\bs\u0010fR\u0014\u0010t\u001a\b\u0012\u0004\u0012\u00020u0qX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010v\u001a\b\u0012\u0004\u0012\u00020u0c\u00a2\u0006\b\n\u0000\u001a\u0004\bw\u0010fR\u0014\u0010x\u001a\b\u0012\u0004\u0012\u00020d0qX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010y\u001a\b\u0012\u0004\u0012\u00020d0c\u00a2\u0006\b\n\u0000\u001a\u0004\by\u0010fR\u0019\u0010z\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010{0c\u00a2\u0006\b\n\u0000\u001a\u0004\b|\u0010fR\u001e\u0010}\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u007f0~0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u0080\u0001\u0010fR \u0010\u0081\u0001\u001a\u000f\u0012\u000b\u0012\t\u0012\u0005\u0012\u00030\u0082\u00010~0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u0083\u0001\u0010fR \u0010\u0084\u0001\u001a\u000f\u0012\u000b\u0012\t\u0012\u0005\u0012\u00030\u0085\u00010~0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u0086\u0001\u0010fR\u001a\u0010\u0087\u0001\u001a\t\u0012\u0005\u0012\u00030\u0088\u00010c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u0089\u0001\u0010fR\u001c\u0010\u008a\u0001\u001a\u000b\u0012\u0007\u0012\u0005\u0018\u00010\u0085\u00010c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u008b\u0001\u0010fR\u001c\u0010\u008c\u0001\u001a\u000b\u0012\u0007\u0012\u0005\u0018\u00010\u008d\u00010c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u008e\u0001\u0010fR\u001a\u0010\u008f\u0001\u001a\t\u0012\u0005\u0012\u00030\u0090\u00010c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u0091\u0001\u0010fR\u001a\u0010\u0092\u0001\u001a\t\u0012\u0005\u0012\u00030\u0093\u00010c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u0094\u0001\u0010fR\u001b\u0010\u0095\u0001\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010u0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u0096\u0001\u0010fR \u0010\u0097\u0001\u001a\u000f\u0012\u000b\u0012\t\u0012\u0005\u0012\u00030\u008d\u00010~0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u0098\u0001\u0010fR\u0019\u0010\u0099\u0001\u001a\b\u0012\u0004\u0012\u00020d0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u009a\u0001\u0010fR\u0019\u0010\u009b\u0001\u001a\b\u0012\u0004\u0012\u00020d0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u009c\u0001\u0010fR\u0019\u0010\u009d\u0001\u001a\b\u0012\u0004\u0012\u00020u0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u009e\u0001\u0010fR\u0019\u0010\u009f\u0001\u001a\b\u0012\u0004\u0012\u00020{0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00a0\u0001\u0010fR \u0010\u00a1\u0001\u001a\u000f\u0012\u000b\u0012\t\u0012\u0005\u0012\u00030\u00a2\u00010~0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00a3\u0001\u0010fR\u0019\u0010\u00a4\u0001\u001a\b\u0012\u0004\u0012\u00020d0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00a4\u0001\u0010fR\u0019\u0010\u00a5\u0001\u001a\b\u0012\u0004\u0012\u00020u0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00a6\u0001\u0010fR\u001f\u0010\u00a7\u0001\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020u0~0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00a8\u0001\u0010fR\u001a\u0010\u00a9\u0001\u001a\t\u0012\u0005\u0012\u00030\u00aa\u00010c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00ab\u0001\u0010fR\u0019\u0010\u00ac\u0001\u001a\b\u0012\u0004\u0012\u00020{0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00ad\u0001\u0010fR \u0010\u00ae\u0001\u001a\u000f\u0012\u000b\u0012\t\u0012\u0005\u0012\u00030\u00af\u00010~0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00b0\u0001\u0010fR\u0019\u0010\u00b1\u0001\u001a\b\u0012\u0004\u0012\u00020d0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00b1\u0001\u0010fR\u0019\u0010\u00b2\u0001\u001a\b\u0012\u0004\u0012\u00020u0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00b3\u0001\u0010fR\u0019\u0010\u00b4\u0001\u001a\b\u0012\u0004\u0012\u00020u0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00b5\u0001\u0010fR\u001c\u0010\u00b6\u0001\u001a\u000b\u0012\u0007\u0012\u0005\u0018\u00010\u00b7\u00010c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00b8\u0001\u0010fR\u0017\u0010\u00b9\u0001\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010u0qX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u00ba\u0001\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010u0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00bb\u0001\u0010fR\u0019\u0010\u00bc\u0001\u001a\b\u0012\u0004\u0012\u00020u0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00bd\u0001\u0010fR \u0010\u00be\u0001\u001a\u000f\u0012\u000b\u0012\t\u0012\u0005\u0012\u00030\u00bf\u00010~0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00c0\u0001\u0010fR \u0010\u00c1\u0001\u001a\u000f\u0012\u000b\u0012\t\u0012\u0005\u0012\u00030\u00c2\u00010~0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00c3\u0001\u0010fR\u001a\u0010\u00c4\u0001\u001a\t\u0012\u0005\u0012\u00030\u00c5\u00010c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00c6\u0001\u0010fR\u001c\u0010\u00c7\u0001\u001a\u000b\u0012\u0007\u0012\u0005\u0018\u00010\u00c2\u00010c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00c8\u0001\u0010fR\u0019\u0010\u00c9\u0001\u001a\b\u0012\u0004\u0012\u00020d0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00c9\u0001\u0010fR\u0019\u0010\u00ca\u0001\u001a\b\u0012\u0004\u0012\u00020d0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00ca\u0001\u0010fR\u001c\u0010\u00cb\u0001\u001a\u000b\u0012\u0007\u0012\u0005\u0018\u00010\u00cc\u00010c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00cd\u0001\u0010fR\u001f\u0010\u00ce\u0001\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020u0~0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00cf\u0001\u0010fR\u0019\u0010\u00d0\u0001\u001a\b\u0012\u0004\u0012\u00020d0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00d0\u0001\u0010fR\u0019\u0010\u00d1\u0001\u001a\b\u0012\u0004\u0012\u00020d0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00d1\u0001\u0010fR \u0010\u00d2\u0001\u001a\u000f\u0012\u000b\u0012\t\u0012\u0005\u0012\u00030\u00d3\u00010~0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00d4\u0001\u0010fR\u001f\u0010\u00d5\u0001\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020u0~0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00d6\u0001\u0010fR\u001a\u0010\u00d7\u0001\u001a\t\u0012\u0005\u0012\u00030\u00d8\u00010c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00d9\u0001\u0010fR\u0016\u0010\u00da\u0001\u001a\t\u0012\u0005\u0012\u00030\u00db\u00010qX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u00dc\u0001\u001a\t\u0012\u0005\u0012\u00030\u00db\u00010c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00dd\u0001\u0010fR\u0016\u0010\u00de\u0001\u001a\t\u0012\u0005\u0012\u00030\u00df\u00010qX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u00e0\u0001\u001a\t\u0012\u0005\u0012\u00030\u00df\u00010c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00e1\u0001\u0010fR \u0010\u00e2\u0001\u001a\u000f\u0012\u000b\u0012\t\u0012\u0005\u0012\u00030\u00e3\u00010~0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00e4\u0001\u0010fR \u0010\u00e5\u0001\u001a\u000f\u0012\u000b\u0012\t\u0012\u0005\u0012\u00030\u00e6\u00010~0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00e7\u0001\u0010fR \u0010\u00e8\u0001\u001a\u000f\u0012\u000b\u0012\t\u0012\u0005\u0012\u00030\u00e9\u00010~0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00ea\u0001\u0010fR\u001a\u0010\u00eb\u0001\u001a\t\u0012\u0005\u0012\u00030\u00ec\u00010c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00ed\u0001\u0010fR.\u0010\u00ee\u0001\u001a\u001d\u0012\u0019\u0012\u0017\u0012\u0013\u0012\u0011\u0012\u0005\u0012\u00030\u00f0\u0001\u0012\u0005\u0012\u00030\u00f0\u00010\u00ef\u00010~0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00f1\u0001\u0010fR\u0019\u0010\u00f2\u0001\u001a\b\u0012\u0004\u0012\u00020d0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00f2\u0001\u0010fR\u0015\u0010\u00f3\u0001\u001a\b\u0012\u0004\u0012\u00020u0qX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u00f4\u0001\u001a\b\u0012\u0004\u0012\u00020u0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00f5\u0001\u0010fR\u0015\u0010\u00f8\u0001\u001a\b\u0012\u0004\u0012\u00020u0qX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u00f9\u0001\u001a\b\u0012\u0004\u0012\u00020u0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00fa\u0001\u0010fR\u0015\u0010\u00fe\u0001\u001a\b\u0012\u0004\u0012\u00020d0qX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u00ff\u0001\u001a\b\u0012\u0004\u0012\u00020d0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u0080\u0002\u0010fR\u0015\u0010\u0085\u0002\u001a\b\u0012\u0004\u0012\u00020d0qX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u0086\u0002\u001a\b\u0012\u0004\u0012\u00020d0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u0087\u0002\u0010fR\u0015\u0010\u0089\u0002\u001a\b\u0012\u0004\u0012\u00020d0qX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u008a\u0002\u001a\b\u0012\u0004\u0012\u00020d0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u008b\u0002\u0010fR\u0015\u0010\u008d\u0002\u001a\b\u0012\u0004\u0012\u00020u0qX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u008e\u0002\u001a\b\u0012\u0004\u0012\u00020u0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u008f\u0002\u0010fR\u0015\u0010\u0092\u0002\u001a\b\u0012\u0004\u0012\u00020d0qX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u0093\u0002\u001a\b\u0012\u0004\u0012\u00020d0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u0094\u0002\u0010fR\u0015\u0010\u0096\u0002\u001a\b\u0012\u0004\u0012\u00020d0qX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u0097\u0002\u001a\b\u0012\u0004\u0012\u00020d0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u0098\u0002\u0010fR\u0015\u0010\u009a\u0002\u001a\b\u0012\u0004\u0012\u00020d0qX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u009b\u0002\u001a\b\u0012\u0004\u0012\u00020d0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u009c\u0002\u0010fR\u0015\u0010\u009e\u0002\u001a\b\u0012\u0004\u0012\u00020d0qX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u009f\u0002\u001a\b\u0012\u0004\u0012\u00020d0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00a0\u0002\u0010fR\u0015\u0010\u00a1\u0002\u001a\b\u0012\u0004\u0012\u00020u0qX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u00a2\u0002\u001a\b\u0012\u0004\u0012\u00020u0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00a3\u0002\u0010fR\u0015\u0010\u00a6\u0002\u001a\b\u0012\u0004\u0012\u00020u0qX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u00a7\u0002\u001a\b\u0012\u0004\u0012\u00020u0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00a8\u0002\u0010fR\u0015\u0010\u00aa\u0002\u001a\b\u0012\u0004\u0012\u00020d0qX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u00ab\u0002\u001a\b\u0012\u0004\u0012\u00020d0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00ab\u0002\u0010fR\u0015\u0010\u00ac\u0002\u001a\b\u0012\u0004\u0012\u00020u0qX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u00ad\u0002\u001a\b\u0012\u0004\u0012\u00020u0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00ae\u0002\u0010fR\u0017\u0010\u00b7\u0002\u001a\n\u0012\u0005\u0012\u00030\u00b9\u00020\u00b8\u0002X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001c\u0010\u00ba\u0002\u001a\n\u0012\u0005\u0012\u00030\u00b9\u00020\u00bb\u0002\u00a2\u0006\n\n\u0000\u001a\u0006\b\u00bc\u0002\u0010\u00bd\u0002R\u0016\u0010\u00be\u0002\u001a\t\u0012\u0005\u0012\u00030\u00bf\u00020qX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u00c0\u0002\u001a\t\u0012\u0005\u0012\u00030\u00bf\u00020c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00c1\u0002\u0010fR\u0014\u0010\u00c5\u0002\u001a\u00020d8F\u00a2\u0006\b\u001a\u0006\b\u00c5\u0002\u0010\u00c6\u0002R\u0019\u0010\u00b0\u0003\u001a\b\u0012\u0004\u0012\u00020d0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00b0\u0003\u0010fR\u0019\u0010\u00b1\u0003\u001a\b\u0012\u0004\u0012\u00020d0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00b1\u0003\u0010fR\u001a\u0010\u00b6\u0003\u001a\b\u0012\u0004\u0012\u00020d0q\u00a2\u0006\n\n\u0000\u001a\u0006\b\u00b6\u0003\u0010\u00b7\u0003R\u001a\u0010\u00b8\u0003\u001a\b\u0012\u0004\u0012\u00020d0q\u00a2\u0006\n\n\u0000\u001a\u0006\b\u00b8\u0003\u0010\u00b7\u0003R \u0010\u00bd\u0003\u001a\u000f\u0012\u000b\u0012\t\u0012\u0005\u0012\u00030\u00be\u00030~0c\u00a2\u0006\t\n\u0000\u001a\u0005\b\u00bf\u0003\u0010f\u00a8\u0006\u00cd\u0003"}, d2={"Lcom/example/ui/GuardianViewModel;", "Landroidx/lifecycle/AndroidViewModel;", "application", "Landroid/app/Application;", "authService", "Lcom/example/service/AuthService;", "databaseService", "Lcom/example/service/DatabaseService;", "locationService", "Lcom/example/service/LocationService;", "alarmVibratorService", "Lcom/example/service/AlarmVibratorService;", "notificationService", "Lcom/example/service/NotificationService;", "notificationProvider", "Lcom/example/service/NotificationProvider;", "historyService", "Lcom/example/service/HistoryService;", "historyProvider", "Lcom/example/service/HistoryProvider;", "aiAnalysisService", "Lcom/example/service/AiAnalysisService;", "deviceService", "Lcom/example/service/DeviceService;", "fallDatabase", "Lcom/example/data/FallDatabase;", "fallRepository", "Lcom/example/repository/FallRepository;", "fallDetectionService", "Lcom/example/service/FallDetectionService;", "voiceSosService", "Lcom/example/service/VoiceSosService;", "aiService", "Lcom/example/service/AIService;", "aiProvider", "Lcom/example/service/AIProvider;", "emergencyService", "Lcom/example/service/EmergencyService;", "emergencyProvider", "Lcom/example/service/EmergencyProvider;", "safetyTimerService", "Lcom/example/service/SafetyTimerService;", "analyticsService", "Lcom/example/service/AnalyticsService;", "securityService", "Lcom/example/service/SecurityService;", "trustedPlacesService", "Lcom/example/service/TrustedPlacesService;", "settingsDataStore", "Lcom/example/data/SettingsDataStore;", "<init>", "(Landroid/app/Application;Lcom/example/service/AuthService;Lcom/example/service/DatabaseService;Lcom/example/service/LocationService;Lcom/example/service/AlarmVibratorService;Lcom/example/service/NotificationService;Lcom/example/service/NotificationProvider;Lcom/example/service/HistoryService;Lcom/example/service/HistoryProvider;Lcom/example/service/AiAnalysisService;Lcom/example/service/DeviceService;Lcom/example/data/FallDatabase;Lcom/example/repository/FallRepository;Lcom/example/service/FallDetectionService;Lcom/example/service/VoiceSosService;Lcom/example/service/AIService;Lcom/example/service/AIProvider;Lcom/example/service/EmergencyService;Lcom/example/service/EmergencyProvider;Lcom/example/service/SafetyTimerService;Lcom/example/service/AnalyticsService;Lcom/example/service/SecurityService;Lcom/example/service/TrustedPlacesService;Lcom/example/data/SettingsDataStore;)V", "getAuthService", "()Lcom/example/service/AuthService;", "getDatabaseService", "()Lcom/example/service/DatabaseService;", "getLocationService", "()Lcom/example/service/LocationService;", "getAlarmVibratorService", "()Lcom/example/service/AlarmVibratorService;", "getNotificationService", "()Lcom/example/service/NotificationService;", "getNotificationProvider", "()Lcom/example/service/NotificationProvider;", "getHistoryService", "()Lcom/example/service/HistoryService;", "getHistoryProvider", "()Lcom/example/service/HistoryProvider;", "getAiAnalysisService", "()Lcom/example/service/AiAnalysisService;", "getDeviceService", "()Lcom/example/service/DeviceService;", "getFallDatabase", "()Lcom/example/data/FallDatabase;", "getFallRepository", "()Lcom/example/repository/FallRepository;", "getFallDetectionService", "()Lcom/example/service/FallDetectionService;", "getVoiceSosService", "()Lcom/example/service/VoiceSosService;", "getAiService", "()Lcom/example/service/AIService;", "getAiProvider", "()Lcom/example/service/AIProvider;", "getEmergencyService", "()Lcom/example/service/EmergencyService;", "getEmergencyProvider", "()Lcom/example/service/EmergencyProvider;", "getSafetyTimerService", "()Lcom/example/service/SafetyTimerService;", "getAnalyticsService", "()Lcom/example/service/AnalyticsService;", "getSecurityService", "()Lcom/example/service/SecurityService;", "getTrustedPlacesService", "()Lcom/example/service/TrustedPlacesService;", "getSettingsDataStore", "()Lcom/example/data/SettingsDataStore;", "developerModeEnabled", "Lkotlinx/coroutines/flow/StateFlow;", "", "getDeveloperModeEnabled", "()Lkotlinx/coroutines/flow/StateFlow;", "setDeveloperModeEnabled", "", "enabled", "sosSoundEnabled", "getSosSoundEnabled", "sosVibrationEnabled", "getSosVibrationEnabled", "setSosSoundEnabled", "setSosVibrationEnabled", "_voiceSosEnabled", "Lkotlinx/coroutines/flow/MutableStateFlow;", "voiceSosEnabled", "getVoiceSosEnabled", "_voiceSosPhrase", "", "voiceSosPhrase", "getVoiceSosPhrase", "_isSirenPlaying", "isSirenPlaying", "countdown", "", "getCountdown", "notifications", "", "Lcom/example/model/NotificationItem;", "getNotifications", "notificationsNew", "Lcom/example/model/NotificationModel;", "getNotificationsNew", "aiLogsNew", "Lcom/example/model/AIAnalysisModel;", "getAiLogsNew", "currentLiveReadingNew", "Lcom/example/model/AISensorReading;", "getCurrentLiveReadingNew", "currentLiveAnalysisNew", "getCurrentLiveAnalysisNew", "mpuReading", "Lcom/example/ble/Mpu6050Reading;", "getMpuReading", "mpuHardwareState", "Lcom/example/ble/MpuHardwareState;", "getMpuHardwareState", "mpuMotionState", "Lcom/example/ble/MotionState;", "getMpuMotionState", "mpuRawString", "getMpuRawString", "mpuRecentReadings", "getMpuRecentReadings", "mpuCharacteristicFound", "getMpuCharacteristicFound", "mpuNotificationSubscribed", "getMpuNotificationSubscribed", "fallState", "getFallState", "fallCountdown", "getFallCountdown", "allFallEvents", "Lcom/example/model/FallEvent;", "getAllFallEvents", "isVoiceListening", "voiceState", "getVoiceState", "wakePhrases", "getWakePhrases", "micDecibels", "", "getMicDecibels", "voiceConfidenceThreshold", "getVoiceConfidenceThreshold", "voiceActivationLogs", "Lcom/example/service/VoiceActivationLog;", "getVoiceActivationLogs", "isSpeechRecognizerActive", "liveSpokenText", "getLiveSpokenText", "speechStatusMessage", "getSpeechStatusMessage", "lastRecognizedCommand", "Lcom/example/service/VoiceCommand;", "getLastRecognizedCommand", "_voiceCommandConfirmation", "voiceCommandConfirmation", "getVoiceCommandConfirmation", "fcmToken", "getFcmToken", "emergencyHistory", "Lcom/example/model/HistoryModel;", "getEmergencyHistory", "aiLogs", "Lcom/example/model/AiAnalysisResult;", "getAiLogs", "currentLiveReading", "Lcom/example/model/SensorReading;", "getCurrentLiveReading", "currentLiveAnalysis", "getCurrentLiveAnalysis", "isRefreshingDevices", "isEsp32Connected", "activeEmergency", "Lcom/example/model/EmergencyModel;", "getActiveEmergency", "diagnosticsLog", "getDiagnosticsLog", "isDiagnosingDevice", "isNetworkAvailable", "trustedPlaces", "Lcom/example/model/TrustedPlace;", "getTrustedPlaces", "esp32CommLogs", "getEsp32CommLogs", "authState", "Lcom/example/service/AuthState;", "getAuthState", "_sosWorkflowState", "Lcom/example/model/SosWorkflowState;", "sosWorkflowState", "getSosWorkflowState", "_emergencySession", "Lcom/example/model/EmergencySession;", "emergencySession", "getEmergencySession", "alerts", "Lcom/example/model/Alert;", "getAlerts", "devices", "Lcom/example/model/Device;", "getDevices", "contacts", "Lcom/example/model/EmergencyContact;", "getContacts", "currentLocation", "Lcom/example/model/UserLocation;", "getCurrentLocation", "routePoints", "Lkotlin/Pair;", "", "getRoutePoints", "isTrackingLocation", "_themeMode", "themeMode", "getThemeMode", "setThemeMode", "mode", "_language", "language", "getLanguage", "setLanguage", "lang", "toggleSirenAlarm", "_criticalAlarmsEnabled", "criticalAlarmsEnabled", "getCriticalAlarmsEnabled", "setVoiceSosEnabled", "setVoiceSosPhrase", "phrase", "setCriticalAlarmsEnabled", "_arrivalAlertsEnabled", "arrivalAlertsEnabled", "getArrivalAlertsEnabled", "setArrivalAlertsEnabled", "_deviceStatusNotificationsEnabled", "deviceStatusNotificationsEnabled", "getDeviceStatusNotificationsEnabled", "setDeviceStatusNotificationsEnabled", "_locationSharingInterval", "locationSharingInterval", "getLocationSharingInterval", "setLocationSharingInterval", "interval", "_backgroundLocationEnabled", "backgroundLocationEnabled", "getBackgroundLocationEnabled", "setBackgroundLocationEnabled", "_telemetrySharingEnabled", "telemetrySharingEnabled", "getTelemetrySharingEnabled", "setTelemetrySharingEnabled", "_biometricEnabled", "biometricEnabled", "getBiometricEnabled", "setBiometricEnabled", "_appLockPinEnabled", "appLockPinEnabled", "getAppLockPinEnabled", "_appLockPin", "appLockPin", "getAppLockPin", "setAppLockPin", "pin", "_emergencyPin", "emergencyPin", "getEmergencyPin", "setEmergencyPin", "_isBackupRunning", "isBackupRunning", "_lastBackupTime", "lastBackupTime", "getLastBackupTime", "runBackup", "runRestore", "changePassword", "old", "new", "callback", "Lkotlin/Function1;", "deleteAccount", "_uiEvents", "Lkotlinx/coroutines/flow/MutableSharedFlow;", "Lcom/example/ui/GuardianViewModel$UiEvent;", "uiEvents", "Lkotlinx/coroutines/flow/SharedFlow;", "getUiEvents", "()Lkotlinx/coroutines/flow/SharedFlow;", "_permissionsState", "Lcom/example/model/PermissionsState;", "permissionsState", "getPermissionsState", "refreshPermissions", "context", "Landroid/content/Context;", "isDemoMode", "()Z", "loginUser", "email", "pass", "registerUser", "name", "phone", "medical", "contactName", "contactPhone", "resetPassword", "logout", "updateUserProfile", "updatedUser", "Lcom/example/model/User;", "checkSystemReadiness", "getMatchedTrustedPlace", "lat", "lng", "initiateEmergencySequence", "triggerSource", "deviceId", "accuracy", "altitude", "speed", "bearing", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Double;Ljava/lang/Double;Ljava/lang/Float;Ljava/lang/Double;Ljava/lang/Float;Ljava/lang/Float;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "triggerTimerSOS", "triggerManualSOS", "triggerFallDetectedSOS", "triggerVoiceSOS", "matchedPhrase", "confidence", "handleVoiceCommand", "command", "startVoiceRecognition", "stopVoiceRecognition", "clearVoiceCommandConfirmation", "resolveAlert", "alertId", "notes", "bondDevice", "mac", "firmware", "battery", "signal", "health", "renameDevice", "newName", "unbondDevice", "saveEmergencyContact", "contact", "deleteEmergencyContact", "contactId", "startLocationTracking", "stopLocationTracking", "saveFavoritePlace", "type", "deleteFavoritePlace", "id", "searchCoordinates", "query", "getCurrentLocationOnce", "Landroid/location/Location;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "addTrustedPlace", "place", "updateTrustedPlace", "deleteTrustedPlace", "placeId", "updateMapOptions", "trafficEnabled", "resetDistance", "searchLocation", "triggerEsp32SOS", "triggerType", "acknowledgeEmergency", "updateResponderStatus", "newStatus", "muteEmergencyAlarm", "markEmergencySafe", "cancelEmergencyWithPin", "endEmergencySOS", "deleteHistoryItem", "getHistoryCSVString", "getHistoryPDFReportText", "markNotificationAsRead", "markNotificationNewAsRead", "markAllNotificationsAsRead", "markAllNotificationsNewAsRead", "deleteNotification", "deleteNotificationNew", "refreshDeviceStatus", "restartDevice", "runDiagnostics", "cleanDiagnosticsLog", "setNetworkAvailable", "available", "addCommLog", "log", "clearCommLogs", "authenticateAndRegisterESP32", "token", "onResult", "Lkotlin/Result;", "resetEsp32", "isGpsDisabled", "isWeakGps", "setGpsDisabled", "disabled", "setWeakGps", "weak", "isOfflineMode", "()Lkotlinx/coroutines/flow/MutableStateFlow;", "isSlowNetwork", "setOfflineMode", "setSlowNetwork", "uploadTestSOS", "downloadTestData", "developerLogs", "Lcom/example/model/DeveloperLog;", "getDeveloperLogs", "addDeveloperLog", "event", "status", "clearDeveloperLogs", "deleteTestRecords", "setCustomLocation", "disconnectDevice", "connectDevice", "onCleared", "startEsp32Polling", "stopEsp32Polling", "triggerManualHeartbeatCheck", "UiEvent", "app"})
@HiltViewModel
@StabilityInferred(parameters=0)
public final class GuardianViewModel
extends AndroidViewModel {
    @NotNull
    private final AuthService authService;
    @NotNull
    private final DatabaseService databaseService;
    @NotNull
    private final LocationService locationService;
    @NotNull
    private final AlarmVibratorService alarmVibratorService;
    @NotNull
    private final NotificationService notificationService;
    @NotNull
    private final NotificationProvider notificationProvider;
    @NotNull
    private final HistoryService historyService;
    @NotNull
    private final HistoryProvider historyProvider;
    @NotNull
    private final AiAnalysisService aiAnalysisService;
    @NotNull
    private final DeviceService deviceService;
    @NotNull
    private final FallDatabase fallDatabase;
    @NotNull
    private final FallRepository fallRepository;
    @NotNull
    private final FallDetectionService fallDetectionService;
    @NotNull
    private final VoiceSosService voiceSosService;
    @NotNull
    private final AIService aiService;
    @NotNull
    private final AIProvider aiProvider;
    @NotNull
    private final EmergencyService emergencyService;
    @NotNull
    private final EmergencyProvider emergencyProvider;
    @NotNull
    private final SafetyTimerService safetyTimerService;
    @NotNull
    private final AnalyticsService analyticsService;
    @NotNull
    private final SecurityService securityService;
    @NotNull
    private final TrustedPlacesService trustedPlacesService;
    @NotNull
    private final SettingsDataStore settingsDataStore;
    @NotNull
    private final StateFlow<Boolean> developerModeEnabled;
    @NotNull
    private final StateFlow<Boolean> sosSoundEnabled;
    @NotNull
    private final StateFlow<Boolean> sosVibrationEnabled;
    @NotNull
    private final MutableStateFlow<Boolean> _voiceSosEnabled;
    @NotNull
    private final StateFlow<Boolean> voiceSosEnabled;
    @NotNull
    private final MutableStateFlow<String> _voiceSosPhrase;
    @NotNull
    private final StateFlow<String> voiceSosPhrase;
    @NotNull
    private final MutableStateFlow<Boolean> _isSirenPlaying;
    @NotNull
    private final StateFlow<Boolean> isSirenPlaying;
    @NotNull
    private final StateFlow<Integer> countdown;
    @NotNull
    private final StateFlow<List<NotificationItem>> notifications;
    @NotNull
    private final StateFlow<List<NotificationModel>> notificationsNew;
    @NotNull
    private final StateFlow<List<AIAnalysisModel>> aiLogsNew;
    @NotNull
    private final StateFlow<AISensorReading> currentLiveReadingNew;
    @NotNull
    private final StateFlow<AIAnalysisModel> currentLiveAnalysisNew;
    @NotNull
    private final StateFlow<Mpu6050Reading> mpuReading;
    @NotNull
    private final StateFlow<MpuHardwareState> mpuHardwareState;
    @NotNull
    private final StateFlow<MotionState> mpuMotionState;
    @NotNull
    private final StateFlow<String> mpuRawString;
    @NotNull
    private final StateFlow<List<Mpu6050Reading>> mpuRecentReadings;
    @NotNull
    private final StateFlow<Boolean> mpuCharacteristicFound;
    @NotNull
    private final StateFlow<Boolean> mpuNotificationSubscribed;
    @NotNull
    private final StateFlow<String> fallState;
    @NotNull
    private final StateFlow<Integer> fallCountdown;
    @NotNull
    private final StateFlow<List<FallEvent>> allFallEvents;
    @NotNull
    private final StateFlow<Boolean> isVoiceListening;
    @NotNull
    private final StateFlow<String> voiceState;
    @NotNull
    private final StateFlow<List<String>> wakePhrases;
    @NotNull
    private final StateFlow<Float> micDecibels;
    @NotNull
    private final StateFlow<Integer> voiceConfidenceThreshold;
    @NotNull
    private final StateFlow<List<VoiceActivationLog>> voiceActivationLogs;
    @NotNull
    private final StateFlow<Boolean> isSpeechRecognizerActive;
    @NotNull
    private final StateFlow<String> liveSpokenText;
    @NotNull
    private final StateFlow<String> speechStatusMessage;
    @NotNull
    private final StateFlow<VoiceCommand> lastRecognizedCommand;
    @NotNull
    private final MutableStateFlow<String> _voiceCommandConfirmation;
    @NotNull
    private final StateFlow<String> voiceCommandConfirmation;
    @NotNull
    private final StateFlow<String> fcmToken;
    @NotNull
    private final StateFlow<List<HistoryModel>> emergencyHistory;
    @NotNull
    private final StateFlow<List<AiAnalysisResult>> aiLogs;
    @NotNull
    private final StateFlow<SensorReading> currentLiveReading;
    @NotNull
    private final StateFlow<AiAnalysisResult> currentLiveAnalysis;
    @NotNull
    private final StateFlow<Boolean> isRefreshingDevices;
    @NotNull
    private final StateFlow<Boolean> isEsp32Connected;
    @NotNull
    private final StateFlow<EmergencyModel> activeEmergency;
    @NotNull
    private final StateFlow<List<String>> diagnosticsLog;
    @NotNull
    private final StateFlow<Boolean> isDiagnosingDevice;
    @NotNull
    private final StateFlow<Boolean> isNetworkAvailable;
    @NotNull
    private final StateFlow<List<TrustedPlace>> trustedPlaces;
    @NotNull
    private final StateFlow<List<String>> esp32CommLogs;
    @NotNull
    private final StateFlow<AuthState> authState;
    @NotNull
    private final MutableStateFlow<SosWorkflowState> _sosWorkflowState;
    @NotNull
    private final StateFlow<SosWorkflowState> sosWorkflowState;
    @NotNull
    private final MutableStateFlow<EmergencySession> _emergencySession;
    @NotNull
    private final StateFlow<EmergencySession> emergencySession;
    @NotNull
    private final StateFlow<List<Alert>> alerts;
    @NotNull
    private final StateFlow<List<Device>> devices;
    @NotNull
    private final StateFlow<List<EmergencyContact>> contacts;
    @NotNull
    private final StateFlow<UserLocation> currentLocation;
    @NotNull
    private final StateFlow<List<Pair<Double, Double>>> routePoints;
    @NotNull
    private final StateFlow<Boolean> isTrackingLocation;
    @NotNull
    private final MutableStateFlow<String> _themeMode;
    @NotNull
    private final StateFlow<String> themeMode;
    @NotNull
    private final MutableStateFlow<String> _language;
    @NotNull
    private final StateFlow<String> language;
    @NotNull
    private final MutableStateFlow<Boolean> _criticalAlarmsEnabled;
    @NotNull
    private final StateFlow<Boolean> criticalAlarmsEnabled;
    @NotNull
    private final MutableStateFlow<Boolean> _arrivalAlertsEnabled;
    @NotNull
    private final StateFlow<Boolean> arrivalAlertsEnabled;
    @NotNull
    private final MutableStateFlow<Boolean> _deviceStatusNotificationsEnabled;
    @NotNull
    private final StateFlow<Boolean> deviceStatusNotificationsEnabled;
    @NotNull
    private final MutableStateFlow<String> _locationSharingInterval;
    @NotNull
    private final StateFlow<String> locationSharingInterval;
    @NotNull
    private final MutableStateFlow<Boolean> _backgroundLocationEnabled;
    @NotNull
    private final StateFlow<Boolean> backgroundLocationEnabled;
    @NotNull
    private final MutableStateFlow<Boolean> _telemetrySharingEnabled;
    @NotNull
    private final StateFlow<Boolean> telemetrySharingEnabled;
    @NotNull
    private final MutableStateFlow<Boolean> _biometricEnabled;
    @NotNull
    private final StateFlow<Boolean> biometricEnabled;
    @NotNull
    private final MutableStateFlow<Boolean> _appLockPinEnabled;
    @NotNull
    private final StateFlow<Boolean> appLockPinEnabled;
    @NotNull
    private final MutableStateFlow<String> _appLockPin;
    @NotNull
    private final StateFlow<String> appLockPin;
    @NotNull
    private final MutableStateFlow<String> _emergencyPin;
    @NotNull
    private final StateFlow<String> emergencyPin;
    @NotNull
    private final MutableStateFlow<Boolean> _isBackupRunning;
    @NotNull
    private final StateFlow<Boolean> isBackupRunning;
    @NotNull
    private final MutableStateFlow<String> _lastBackupTime;
    @NotNull
    private final StateFlow<String> lastBackupTime;
    @NotNull
    private final MutableSharedFlow<UiEvent> _uiEvents;
    @NotNull
    private final SharedFlow<UiEvent> uiEvents;
    @NotNull
    private final MutableStateFlow<PermissionsState> _permissionsState;
    @NotNull
    private final StateFlow<PermissionsState> permissionsState;
    @NotNull
    private final StateFlow<Boolean> isGpsDisabled;
    @NotNull
    private final StateFlow<Boolean> isWeakGps;
    @NotNull
    private final MutableStateFlow<Boolean> isOfflineMode;
    @NotNull
    private final MutableStateFlow<Boolean> isSlowNetwork;
    @NotNull
    private final StateFlow<List<DeveloperLog>> developerLogs;
    public static final int $stable = 8;

    @Inject
    public GuardianViewModel(@NotNull Application application, @NotNull AuthService authService, @NotNull DatabaseService databaseService, @NotNull LocationService locationService, @NotNull AlarmVibratorService alarmVibratorService, @NotNull NotificationService notificationService, @NotNull NotificationProvider notificationProvider, @NotNull HistoryService historyService, @NotNull HistoryProvider historyProvider, @NotNull AiAnalysisService aiAnalysisService, @NotNull DeviceService deviceService, @NotNull FallDatabase fallDatabase, @NotNull FallRepository fallRepository, @NotNull FallDetectionService fallDetectionService, @NotNull VoiceSosService voiceSosService, @NotNull AIService aiService, @NotNull AIProvider aiProvider, @NotNull EmergencyService emergencyService, @NotNull EmergencyProvider emergencyProvider, @NotNull SafetyTimerService safetyTimerService, @NotNull AnalyticsService analyticsService, @NotNull SecurityService securityService, @NotNull TrustedPlacesService trustedPlacesService, @NotNull SettingsDataStore settingsDataStore) {
        boolean bl;
        GuardianViewModel guardianViewModel;
        GuardianViewModel guardianViewModel2;
        GuardianViewModel guardianViewModel3;
        String string;
        GuardianViewModel guardianViewModel4;
        boolean bl2;
        GuardianViewModel guardianViewModel5;
        Intrinsics.checkNotNullParameter((Object)application, (String)"application");
        Intrinsics.checkNotNullParameter((Object)authService, (String)"authService");
        Intrinsics.checkNotNullParameter((Object)databaseService, (String)"databaseService");
        Intrinsics.checkNotNullParameter((Object)locationService, (String)"locationService");
        Intrinsics.checkNotNullParameter((Object)alarmVibratorService, (String)"alarmVibratorService");
        Intrinsics.checkNotNullParameter((Object)notificationService, (String)"notificationService");
        Intrinsics.checkNotNullParameter((Object)notificationProvider, (String)"notificationProvider");
        Intrinsics.checkNotNullParameter((Object)historyService, (String)"historyService");
        Intrinsics.checkNotNullParameter((Object)historyProvider, (String)"historyProvider");
        Intrinsics.checkNotNullParameter((Object)aiAnalysisService, (String)"aiAnalysisService");
        Intrinsics.checkNotNullParameter((Object)deviceService, (String)"deviceService");
        Intrinsics.checkNotNullParameter((Object)((Object)fallDatabase), (String)"fallDatabase");
        Intrinsics.checkNotNullParameter((Object)fallRepository, (String)"fallRepository");
        Intrinsics.checkNotNullParameter((Object)fallDetectionService, (String)"fallDetectionService");
        Intrinsics.checkNotNullParameter((Object)voiceSosService, (String)"voiceSosService");
        Intrinsics.checkNotNullParameter((Object)aiService, (String)"aiService");
        Intrinsics.checkNotNullParameter((Object)aiProvider, (String)"aiProvider");
        Intrinsics.checkNotNullParameter((Object)emergencyService, (String)"emergencyService");
        Intrinsics.checkNotNullParameter((Object)emergencyProvider, (String)"emergencyProvider");
        Intrinsics.checkNotNullParameter((Object)safetyTimerService, (String)"safetyTimerService");
        Intrinsics.checkNotNullParameter((Object)analyticsService, (String)"analyticsService");
        Intrinsics.checkNotNullParameter((Object)securityService, (String)"securityService");
        Intrinsics.checkNotNullParameter((Object)trustedPlacesService, (String)"trustedPlacesService");
        Intrinsics.checkNotNullParameter((Object)settingsDataStore, (String)"settingsDataStore");
        super(application);
        this.authService = authService;
        this.databaseService = databaseService;
        this.locationService = locationService;
        this.alarmVibratorService = alarmVibratorService;
        this.notificationService = notificationService;
        this.notificationProvider = notificationProvider;
        this.historyService = historyService;
        this.historyProvider = historyProvider;
        this.aiAnalysisService = aiAnalysisService;
        this.deviceService = deviceService;
        this.fallDatabase = fallDatabase;
        this.fallRepository = fallRepository;
        this.fallDetectionService = fallDetectionService;
        this.voiceSosService = voiceSosService;
        this.aiService = aiService;
        this.aiProvider = aiProvider;
        this.emergencyService = emergencyService;
        this.emergencyProvider = emergencyProvider;
        this.safetyTimerService = safetyTimerService;
        this.analyticsService = analyticsService;
        this.securityService = securityService;
        this.trustedPlacesService = trustedPlacesService;
        this.settingsDataStore = settingsDataStore;
        this.developerModeEnabled = FlowKt.stateIn(this.settingsDataStore.getDeveloperModeFlow(), (CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), (SharingStarted)SharingStarted.Companion.WhileSubscribed$default((SharingStarted.Companion)SharingStarted.Companion, (long)5000L, (long)0L, (int)2, null), (Object)false);
        this.fallDetectionService.setOnSosTriggeredCallback((Function0<Unit>)((Function0)() -> GuardianViewModel._init_$lambda$0(this)));
        this.deviceService.getBleManager().getMotionProcessor().setOnPossibleFallDetected((Function2<? super Mpu6050Reading, ? super String, Unit>)((Function2)(arg_0, arg_1) -> GuardianViewModel._init_$lambda$1(this, arg_0, arg_1)));
        this.sosSoundEnabled = FlowKt.stateIn(this.settingsDataStore.getSosSoundEnabledFlow(), (CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), (SharingStarted)SharingStarted.Companion.WhileSubscribed$default((SharingStarted.Companion)SharingStarted.Companion, (long)5000L, (long)0L, (int)2, null), (Object)true);
        this.sosVibrationEnabled = FlowKt.stateIn(this.settingsDataStore.getSosVibrationEnabledFlow(), (CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), (SharingStarted)SharingStarted.Companion.WhileSubscribed$default((SharingStarted.Companion)SharingStarted.Companion, (long)5000L, (long)0L, (int)2, null), (Object)true);
        GuardianViewModel guardianViewModel6 = this;
        try {
            guardianViewModel5 = guardianViewModel6;
            bl2 = this.getApplication().getSharedPreferences("smart_sos_settings", 0).getBoolean("voice_sos_enabled", false);
        }
        catch (Exception exception) {
            guardianViewModel5 = guardianViewModel6;
            bl2 = false;
        }
        guardianViewModel5._voiceSosEnabled = StateFlowKt.MutableStateFlow((Object)bl2);
        this.voiceSosEnabled = FlowKt.asStateFlow(this._voiceSosEnabled);
        guardianViewModel6 = this;
        try {
            guardianViewModel4 = guardianViewModel6;
            String string2 = this.getApplication().getSharedPreferences("smart_sos_settings", 0).getString("voice_sos_phrase", "Emergency SOS");
            if (string2 == null) {
                string2 = "Emergency SOS";
            }
            string = string2;
        }
        catch (Exception e) {
            guardianViewModel4 = guardianViewModel6;
            string = "Emergency SOS";
        }
        guardianViewModel4._voiceSosPhrase = StateFlowKt.MutableStateFlow((Object)string);
        this.voiceSosPhrase = FlowKt.asStateFlow(this._voiceSosPhrase);
        this._isSirenPlaying = StateFlowKt.MutableStateFlow((Object)false);
        this.isSirenPlaying = FlowKt.asStateFlow(this._isSirenPlaying);
        this.countdown = this.emergencyService.getCountdown();
        this.notifications = this.notificationService.getNotifications();
        this.notificationsNew = this.notificationProvider.getNotifications();
        this.aiLogsNew = this.aiProvider.getAnalysisLogs();
        this.currentLiveReadingNew = this.aiProvider.getCurrentLiveReading();
        this.currentLiveAnalysisNew = this.aiProvider.getCurrentLiveAnalysis();
        this.mpuReading = this.deviceService.getBleManager().getLatestMpuReading();
        this.mpuHardwareState = this.deviceService.getBleManager().getMpuHardwareState();
        this.mpuMotionState = this.deviceService.getBleManager().getMotionState();
        this.mpuRawString = this.deviceService.getBleManager().getMpuRawString();
        this.mpuRecentReadings = this.deviceService.getBleManager().getMpuRecentReadings();
        this.mpuCharacteristicFound = this.deviceService.getBleManager().getMpuCharacteristicFound();
        this.mpuNotificationSubscribed = this.deviceService.getBleManager().getMpuNotificationSubscribed();
        this.fallState = this.fallDetectionService.getCurrentState();
        this.fallCountdown = this.fallDetectionService.getCountdownSeconds();
        this.allFallEvents = FlowKt.stateIn(this.fallRepository.getAllEvents(), (CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), (SharingStarted)SharingStarted.Companion.WhileSubscribed$default((SharingStarted.Companion)SharingStarted.Companion, (long)5000L, (long)0L, (int)2, null), (Object)CollectionsKt.emptyList());
        this.isVoiceListening = this.voiceSosService.isListening();
        this.voiceState = this.voiceSosService.getVoiceState();
        this.wakePhrases = this.voiceSosService.getWakePhrases();
        this.micDecibels = this.voiceSosService.getMicDecibels();
        this.voiceConfidenceThreshold = this.voiceSosService.getConfidenceThreshold();
        this.voiceActivationLogs = this.voiceSosService.getActivationLogs();
        this.isSpeechRecognizerActive = this.voiceSosService.isSpeechRecognizerActive();
        this.liveSpokenText = this.voiceSosService.getLiveSpokenText();
        this.speechStatusMessage = this.voiceSosService.getSpeechStatusMessage();
        this.lastRecognizedCommand = this.voiceSosService.getLastRecognizedCommand();
        this._voiceCommandConfirmation = StateFlowKt.MutableStateFlow(null);
        this.voiceCommandConfirmation = FlowKt.asStateFlow(this._voiceCommandConfirmation);
        this.fcmToken = this.notificationService.getFcmToken();
        this.emergencyHistory = this.historyService.getHistory();
        this.aiLogs = this.aiAnalysisService.getAnalysisLogs();
        this.currentLiveReading = this.aiAnalysisService.getCurrentLiveReading();
        this.currentLiveAnalysis = this.aiAnalysisService.getCurrentLiveAnalysis();
        this.isRefreshingDevices = this.deviceService.isRefreshing();
        this.isEsp32Connected = this.deviceService.isEsp32Connected();
        this.activeEmergency = this.emergencyService.getActiveEmergency();
        this.diagnosticsLog = this.deviceService.getDiagnosticsLog();
        this.isDiagnosingDevice = this.deviceService.isDiagnosing();
        this.isNetworkAvailable = this.deviceService.isNetworkAvailable();
        this.trustedPlaces = this.trustedPlacesService.getTrustedPlaces();
        this.esp32CommLogs = this.deviceService.getEsp32CommLogs();
        this.authState = this.authService.getAuthState();
        this._sosWorkflowState = StateFlowKt.MutableStateFlow((Object)((Object)SosWorkflowState.IDLE));
        this.sosWorkflowState = FlowKt.asStateFlow(this._sosWorkflowState);
        this._emergencySession = StateFlowKt.MutableStateFlow((Object)new EmergencySession(null, null, 0, null, null, null, 0, 0L, null, false, false, false, 4095, null));
        this.emergencySession = FlowKt.asStateFlow(this._emergencySession);
        this.alerts = this.databaseService.getAlerts();
        this.devices = this.databaseService.getDevices();
        this.contacts = this.databaseService.getContacts();
        this.currentLocation = this.locationService.getCurrentLocation();
        this.routePoints = this.locationService.getRoutePoints();
        this.isTrackingLocation = this.locationService.isTracking();
        guardianViewModel6 = this;
        try {
            guardianViewModel3 = guardianViewModel6;
            String string3 = this.getApplication().getSharedPreferences("smart_sos_settings", 0).getString("theme_mode", "SYSTEM");
            if (string3 == null) {
                string3 = "SYSTEM";
            }
            string = string3;
        }
        catch (Exception e) {
            guardianViewModel3 = guardianViewModel6;
            string = "SYSTEM";
        }
        guardianViewModel3._themeMode = StateFlowKt.MutableStateFlow((Object)string);
        this.themeMode = FlowKt.asStateFlow(this._themeMode);
        guardianViewModel6 = this;
        try {
            guardianViewModel2 = guardianViewModel6;
            String string4 = this.getApplication().getSharedPreferences("smart_sos_settings", 0).getString("language", "en");
            if (string4 == null) {
                string4 = "en";
            }
            string = string4;
        }
        catch (Exception e) {
            guardianViewModel2 = guardianViewModel6;
            string = "en";
        }
        guardianViewModel2._language = StateFlowKt.MutableStateFlow((Object)string);
        this.language = FlowKt.asStateFlow(this._language);
        guardianViewModel6 = this;
        try {
            guardianViewModel = guardianViewModel6;
            bl = this.getApplication().getSharedPreferences("smart_sos_settings", 0).getBoolean("critical_alarms_enabled", true);
        }
        catch (Exception e) {
            guardianViewModel = guardianViewModel6;
            bl = true;
        }
        guardianViewModel._criticalAlarmsEnabled = StateFlowKt.MutableStateFlow((Object)bl);
        this.criticalAlarmsEnabled = FlowKt.asStateFlow(this._criticalAlarmsEnabled);
        this._arrivalAlertsEnabled = StateFlowKt.MutableStateFlow((Object)true);
        this.arrivalAlertsEnabled = FlowKt.asStateFlow(this._arrivalAlertsEnabled);
        this._deviceStatusNotificationsEnabled = StateFlowKt.MutableStateFlow((Object)true);
        this.deviceStatusNotificationsEnabled = FlowKt.asStateFlow(this._deviceStatusNotificationsEnabled);
        this._locationSharingInterval = StateFlowKt.MutableStateFlow((Object)"10s");
        this.locationSharingInterval = FlowKt.asStateFlow(this._locationSharingInterval);
        this._backgroundLocationEnabled = StateFlowKt.MutableStateFlow((Object)true);
        this.backgroundLocationEnabled = FlowKt.asStateFlow(this._backgroundLocationEnabled);
        this._telemetrySharingEnabled = StateFlowKt.MutableStateFlow((Object)true);
        this.telemetrySharingEnabled = FlowKt.asStateFlow(this._telemetrySharingEnabled);
        this._biometricEnabled = StateFlowKt.MutableStateFlow((Object)false);
        this.biometricEnabled = FlowKt.asStateFlow(this._biometricEnabled);
        this._appLockPinEnabled = StateFlowKt.MutableStateFlow((Object)false);
        this.appLockPinEnabled = FlowKt.asStateFlow(this._appLockPinEnabled);
        this._appLockPin = StateFlowKt.MutableStateFlow((Object)"");
        this.appLockPin = FlowKt.asStateFlow(this._appLockPin);
        this._emergencyPin = StateFlowKt.MutableStateFlow((Object)this.securityService.getEmergencyPin());
        this.emergencyPin = FlowKt.asStateFlow(this._emergencyPin);
        this._isBackupRunning = StateFlowKt.MutableStateFlow((Object)false);
        this.isBackupRunning = FlowKt.asStateFlow(this._isBackupRunning);
        this._lastBackupTime = StateFlowKt.MutableStateFlow((Object)"Never");
        this.lastBackupTime = FlowKt.asStateFlow(this._lastBackupTime);
        this._uiEvents = SharedFlowKt.MutableSharedFlow$default((int)0, (int)0, null, (int)7, null);
        this.uiEvents = FlowKt.asSharedFlow(this._uiEvents);
        this._permissionsState = StateFlowKt.MutableStateFlow((Object)new PermissionsState(false, false, false, false, false, false, false, false, 255, null));
        this.permissionsState = FlowKt.asStateFlow(this._permissionsState);
        this.isGpsDisabled = this.locationService.isGpsDisabled();
        this.isWeakGps = this.locationService.isWeakGps();
        this.isOfflineMode = this.databaseService.isOfflineMode();
        this.isSlowNetwork = this.databaseService.isSlowNetwork();
        this.developerLogs = this.databaseService.getDeveloperLogs();
        try {
            SharedPreferences prefs = this.getApplication().getSharedPreferences("smart_sos_settings", 0);
            boolean isVoiceEnabled = prefs.getBoolean("voice_sos_enabled", false);
            if (isVoiceEnabled) {
                prefs.edit().putBoolean("voice_sos_enabled", false).apply();
                this._voiceSosEnabled.setValue((Object)false);
            }
        }
        catch (Exception e) {
            Log.e((String)"GuardianViewModel", (String)("Failed to load Voice SOS state safely: " + e.getMessage()));
        }
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            {
                this.this$0 = $receiver;
                super(2, $completion);
            }

            /*
             * Enabled force condition propagation
             * Lifted jumps to return sites
             */
            public final Object invokeSuspend(Object $result) {
                Object object = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        Object object2 = this.this$0.getAuthService().getAuthState().collect(new FlowCollector(){

                            public final Object emit(AuthState state, Continuation<? super Unit> $completion) {
                                if (state instanceof AuthState.Success) {
                                    this$0.getTrustedPlacesService().initialize(((AuthState.Success)state).getUser().getUid());
                                }
                                return Unit.INSTANCE;
                            }
                        }, (Continuation)this);
                        if (object2 != object) throw new KotlinNothingValueException();
                        return object;
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        Object object2 = $result;
                        throw new KotlinNothingValueException();
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
        this.safetyTimerService.setOnTimerExpiredCallback((Function0<Unit>)((Function0)() -> GuardianViewModel._init_$lambda$2(this)));
        this.fallDetectionService.setOnSosTriggeredCallback((Function0<Unit>)((Function0)() -> GuardianViewModel._init_$lambda$3(this)));
        this.voiceSosService.setOnVoiceSosTriggered((Function2<? super String, ? super Integer, Unit>)((Function2)(arg_0, arg_1) -> GuardianViewModel._init_$lambda$4(this, arg_0, arg_1)));
        this.voiceSosService.setOnVoiceCommandRecognized((Function2<? super VoiceCommand, ? super Integer, Unit>)((Function2)(arg_0, arg_1) -> GuardianViewModel._init_$lambda$5(this, arg_0, arg_1)));
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            {
                this.this$0 = $receiver;
                super(2, $completion);
            }

            /*
             * Enabled force condition propagation
             * Lifted jumps to return sites
             */
            public final Object invokeSuspend(Object $result) {
                Object object = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        Object object2 = this.this$0.getEmergencyProvider().getActiveEmergencyState().collect(new FlowCollector(){

                            public final Object emit(EmergencyModel model, Continuation<? super Unit> $completion) {
                                if (model != null) {
                                    Alert alert = ((EmergencySession)this$0._emergencySession.getValue()).getActiveAlert();
                                    if (alert == null) {
                                        alert = new Alert(model.getEmergencyId(), model.getUserId(), model.getUserName(), model.getUserPhone(), model.getLatitude(), model.getLongitude(), "ACTIVE", model.getTriggerType(), model.getStartTimeMs(), 0L, null, null, 3584, null);
                                    }
                                    Alert alert2 = alert;
                                    EmergencySession emergencySession = (EmergencySession)this$0._emergencySession.getValue();
                                    Alert alert3 = Alert.copy$default(alert2, null, null, null, null, model.getLatitude(), model.getLongitude(), model.getStatus(), null, 0L, 0L, null, null, 3983, null);
                                    String string = model.getDeviceId();
                                    long l = model.getStartTimeMs();
                                    String string2 = model.getResponderStatus();
                                    String string3 = Intrinsics.areEqual((Object)model.getTriggerType(), (Object)"FALL_DETECTED") ? "CRITICAL (LEVEL 3)" : "HIGH ALERT (LEVEL 2)";
                                    int n = model.getAiConfidenceScore();
                                    boolean bl = Intrinsics.areEqual((Object)model.getStatus(), (Object)"MARKED_SAFE") || Intrinsics.areEqual((Object)model.getStatus(), (Object)"RESOLVED") || Intrinsics.areEqual((Object)model.getStatus(), (Object)"CANCELLED");
                                    this$0._emergencySession.setValue((Object)EmergencySession.copy$default(emergencySession, alert3, string, 0, null, null, string3, n, l, string2, false, false, bl, 1564, null));
                                }
                                return Unit.INSTANCE;
                            }
                        }, (Continuation)this);
                        if (object2 != object) throw new KotlinNothingValueException();
                        return object;
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        Object object2 = $result;
                        throw new KotlinNothingValueException();
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    @NotNull
    public final AuthService getAuthService() {
        return this.authService;
    }

    @NotNull
    public final DatabaseService getDatabaseService() {
        return this.databaseService;
    }

    @NotNull
    public final LocationService getLocationService() {
        return this.locationService;
    }

    @NotNull
    public final AlarmVibratorService getAlarmVibratorService() {
        return this.alarmVibratorService;
    }

    @NotNull
    public final NotificationService getNotificationService() {
        return this.notificationService;
    }

    @NotNull
    public final NotificationProvider getNotificationProvider() {
        return this.notificationProvider;
    }

    @NotNull
    public final HistoryService getHistoryService() {
        return this.historyService;
    }

    @NotNull
    public final HistoryProvider getHistoryProvider() {
        return this.historyProvider;
    }

    @NotNull
    public final AiAnalysisService getAiAnalysisService() {
        return this.aiAnalysisService;
    }

    @NotNull
    public final DeviceService getDeviceService() {
        return this.deviceService;
    }

    @NotNull
    public final FallDatabase getFallDatabase() {
        return this.fallDatabase;
    }

    @NotNull
    public final FallRepository getFallRepository() {
        return this.fallRepository;
    }

    @NotNull
    public final FallDetectionService getFallDetectionService() {
        return this.fallDetectionService;
    }

    @NotNull
    public final VoiceSosService getVoiceSosService() {
        return this.voiceSosService;
    }

    @NotNull
    public final AIService getAiService() {
        return this.aiService;
    }

    @NotNull
    public final AIProvider getAiProvider() {
        return this.aiProvider;
    }

    @NotNull
    public final EmergencyService getEmergencyService() {
        return this.emergencyService;
    }

    @NotNull
    public final EmergencyProvider getEmergencyProvider() {
        return this.emergencyProvider;
    }

    @NotNull
    public final SafetyTimerService getSafetyTimerService() {
        return this.safetyTimerService;
    }

    @NotNull
    public final AnalyticsService getAnalyticsService() {
        return this.analyticsService;
    }

    @NotNull
    public final SecurityService getSecurityService() {
        return this.securityService;
    }

    @NotNull
    public final TrustedPlacesService getTrustedPlacesService() {
        return this.trustedPlacesService;
    }

    @NotNull
    public final SettingsDataStore getSettingsDataStore() {
        return this.settingsDataStore;
    }

    @NotNull
    public final StateFlow<Boolean> getDeveloperModeEnabled() {
        return this.developerModeEnabled;
    }

    public final void setDeveloperModeEnabled(boolean enabled) {
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, enabled, null){
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            final /* synthetic */ boolean $enabled;
            {
                this.this$0 = $receiver;
                this.$enabled = $enabled;
                super(2, $completion);
            }

            /*
             * Enabled force condition propagation
             * Lifted jumps to return sites
             */
            public final Object invokeSuspend(Object $result) {
                Object object = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        Object object2 = this.this$0.getSettingsDataStore().setDeveloperMode(this.$enabled, (Continuation<? super Unit>)((Continuation)this));
                        if (object2 != object) return Unit.INSTANCE;
                        return object;
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        Object object2 = $result;
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    @NotNull
    public final StateFlow<Boolean> getSosSoundEnabled() {
        return this.sosSoundEnabled;
    }

    @NotNull
    public final StateFlow<Boolean> getSosVibrationEnabled() {
        return this.sosVibrationEnabled;
    }

    public final void setSosSoundEnabled(boolean enabled) {
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, enabled, null){
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            final /* synthetic */ boolean $enabled;
            {
                this.this$0 = $receiver;
                this.$enabled = $enabled;
                super(2, $completion);
            }

            /*
             * Enabled force condition propagation
             * Lifted jumps to return sites
             */
            public final Object invokeSuspend(Object $result) {
                Object object = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        Object object2 = this.this$0.getSettingsDataStore().setSosSoundEnabled(this.$enabled, (Continuation<? super Unit>)((Continuation)this));
                        if (object2 != object) return Unit.INSTANCE;
                        return object;
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        Object object2 = $result;
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void setSosVibrationEnabled(boolean enabled) {
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, enabled, null){
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            final /* synthetic */ boolean $enabled;
            {
                this.this$0 = $receiver;
                this.$enabled = $enabled;
                super(2, $completion);
            }

            /*
             * Enabled force condition propagation
             * Lifted jumps to return sites
             */
            public final Object invokeSuspend(Object $result) {
                Object object = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        Object object2 = this.this$0.getSettingsDataStore().setSosVibrationEnabled(this.$enabled, (Continuation<? super Unit>)((Continuation)this));
                        if (object2 != object) return Unit.INSTANCE;
                        return object;
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        Object object2 = $result;
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    @NotNull
    public final StateFlow<Boolean> getVoiceSosEnabled() {
        return this.voiceSosEnabled;
    }

    @NotNull
    public final StateFlow<String> getVoiceSosPhrase() {
        return this.voiceSosPhrase;
    }

    @NotNull
    public final StateFlow<Boolean> isSirenPlaying() {
        return this.isSirenPlaying;
    }

    @NotNull
    public final StateFlow<Integer> getCountdown() {
        return this.countdown;
    }

    @NotNull
    public final StateFlow<List<NotificationItem>> getNotifications() {
        return this.notifications;
    }

    @NotNull
    public final StateFlow<List<NotificationModel>> getNotificationsNew() {
        return this.notificationsNew;
    }

    @NotNull
    public final StateFlow<List<AIAnalysisModel>> getAiLogsNew() {
        return this.aiLogsNew;
    }

    @NotNull
    public final StateFlow<AISensorReading> getCurrentLiveReadingNew() {
        return this.currentLiveReadingNew;
    }

    @NotNull
    public final StateFlow<AIAnalysisModel> getCurrentLiveAnalysisNew() {
        return this.currentLiveAnalysisNew;
    }

    @NotNull
    public final StateFlow<Mpu6050Reading> getMpuReading() {
        return this.mpuReading;
    }

    @NotNull
    public final StateFlow<MpuHardwareState> getMpuHardwareState() {
        return this.mpuHardwareState;
    }

    @NotNull
    public final StateFlow<MotionState> getMpuMotionState() {
        return this.mpuMotionState;
    }

    @NotNull
    public final StateFlow<String> getMpuRawString() {
        return this.mpuRawString;
    }

    @NotNull
    public final StateFlow<List<Mpu6050Reading>> getMpuRecentReadings() {
        return this.mpuRecentReadings;
    }

    @NotNull
    public final StateFlow<Boolean> getMpuCharacteristicFound() {
        return this.mpuCharacteristicFound;
    }

    @NotNull
    public final StateFlow<Boolean> getMpuNotificationSubscribed() {
        return this.mpuNotificationSubscribed;
    }

    @NotNull
    public final StateFlow<String> getFallState() {
        return this.fallState;
    }

    @NotNull
    public final StateFlow<Integer> getFallCountdown() {
        return this.fallCountdown;
    }

    @NotNull
    public final StateFlow<List<FallEvent>> getAllFallEvents() {
        return this.allFallEvents;
    }

    @NotNull
    public final StateFlow<Boolean> isVoiceListening() {
        return this.isVoiceListening;
    }

    @NotNull
    public final StateFlow<String> getVoiceState() {
        return this.voiceState;
    }

    @NotNull
    public final StateFlow<List<String>> getWakePhrases() {
        return this.wakePhrases;
    }

    @NotNull
    public final StateFlow<Float> getMicDecibels() {
        return this.micDecibels;
    }

    @NotNull
    public final StateFlow<Integer> getVoiceConfidenceThreshold() {
        return this.voiceConfidenceThreshold;
    }

    @NotNull
    public final StateFlow<List<VoiceActivationLog>> getVoiceActivationLogs() {
        return this.voiceActivationLogs;
    }

    @NotNull
    public final StateFlow<Boolean> isSpeechRecognizerActive() {
        return this.isSpeechRecognizerActive;
    }

    @NotNull
    public final StateFlow<String> getLiveSpokenText() {
        return this.liveSpokenText;
    }

    @NotNull
    public final StateFlow<String> getSpeechStatusMessage() {
        return this.speechStatusMessage;
    }

    @NotNull
    public final StateFlow<VoiceCommand> getLastRecognizedCommand() {
        return this.lastRecognizedCommand;
    }

    @NotNull
    public final StateFlow<String> getVoiceCommandConfirmation() {
        return this.voiceCommandConfirmation;
    }

    @NotNull
    public final StateFlow<String> getFcmToken() {
        return this.fcmToken;
    }

    @NotNull
    public final StateFlow<List<HistoryModel>> getEmergencyHistory() {
        return this.emergencyHistory;
    }

    @NotNull
    public final StateFlow<List<AiAnalysisResult>> getAiLogs() {
        return this.aiLogs;
    }

    @NotNull
    public final StateFlow<SensorReading> getCurrentLiveReading() {
        return this.currentLiveReading;
    }

    @NotNull
    public final StateFlow<AiAnalysisResult> getCurrentLiveAnalysis() {
        return this.currentLiveAnalysis;
    }

    @NotNull
    public final StateFlow<Boolean> isRefreshingDevices() {
        return this.isRefreshingDevices;
    }

    @NotNull
    public final StateFlow<Boolean> isEsp32Connected() {
        return this.isEsp32Connected;
    }

    @NotNull
    public final StateFlow<EmergencyModel> getActiveEmergency() {
        return this.activeEmergency;
    }

    @NotNull
    public final StateFlow<List<String>> getDiagnosticsLog() {
        return this.diagnosticsLog;
    }

    @NotNull
    public final StateFlow<Boolean> isDiagnosingDevice() {
        return this.isDiagnosingDevice;
    }

    @NotNull
    public final StateFlow<Boolean> isNetworkAvailable() {
        return this.isNetworkAvailable;
    }

    @NotNull
    public final StateFlow<List<TrustedPlace>> getTrustedPlaces() {
        return this.trustedPlaces;
    }

    @NotNull
    public final StateFlow<List<String>> getEsp32CommLogs() {
        return this.esp32CommLogs;
    }

    @NotNull
    public final StateFlow<AuthState> getAuthState() {
        return this.authState;
    }

    @NotNull
    public final StateFlow<SosWorkflowState> getSosWorkflowState() {
        return this.sosWorkflowState;
    }

    @NotNull
    public final StateFlow<EmergencySession> getEmergencySession() {
        return this.emergencySession;
    }

    @NotNull
    public final StateFlow<List<Alert>> getAlerts() {
        return this.alerts;
    }

    @NotNull
    public final StateFlow<List<Device>> getDevices() {
        return this.devices;
    }

    @NotNull
    public final StateFlow<List<EmergencyContact>> getContacts() {
        return this.contacts;
    }

    @NotNull
    public final StateFlow<UserLocation> getCurrentLocation() {
        return this.currentLocation;
    }

    @NotNull
    public final StateFlow<List<Pair<Double, Double>>> getRoutePoints() {
        return this.routePoints;
    }

    @NotNull
    public final StateFlow<Boolean> isTrackingLocation() {
        return this.isTrackingLocation;
    }

    @NotNull
    public final StateFlow<String> getThemeMode() {
        return this.themeMode;
    }

    public final void setThemeMode(@NotNull String mode) {
        Intrinsics.checkNotNullParameter((Object)mode, (String)"mode");
        this._themeMode.setValue((Object)mode);
        this.databaseService.saveUserSetting("theme_mode", mode);
    }

    @NotNull
    public final StateFlow<String> getLanguage() {
        return this.language;
    }

    public final void setLanguage(@NotNull String lang) {
        Intrinsics.checkNotNullParameter((Object)lang, (String)"lang");
        this._language.setValue((Object)lang);
        this.databaseService.saveUserSetting("language", lang);
    }

    public final void toggleSirenAlarm() {
        Job job;
        if (((Boolean)this._isSirenPlaying.getValue()).booleanValue()) {
            this.alarmVibratorService.stopAlarm();
            this._isSirenPlaying.setValue((Object)false);
            this._emergencySession.setValue((Object)EmergencySession.copy$default((EmergencySession)this._emergencySession.getValue(), null, null, 0, null, null, null, 0, 0L, null, true, false, false, 3583, null));
            job = BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
                int label;
                final /* synthetic */ GuardianViewModel this$0;
                {
                    this.this$0 = $receiver;
                    super(2, $completion);
                }

                /*
                 * Enabled force condition propagation
                 * Lifted jumps to return sites
                 */
                public final Object invokeSuspend(Object $result) {
                    Object object = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                    switch (this.label) {
                        case 0: {
                            ResultKt.throwOnFailure((Object)$result);
                            this.label = 1;
                            Object object2 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Siren alarm silenced."), (Continuation)this);
                            if (object2 != object) return Unit.INSTANCE;
                            return object;
                        }
                        case 1: {
                            ResultKt.throwOnFailure((Object)$result);
                            Object object2 = $result;
                            return Unit.INSTANCE;
                        }
                    }
                    throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                }

                public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                    return (Continuation)new /* invalid duplicate definition of identical inner class */;
                }

                public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                    return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
                }
            }), (int)3, null);
        } else {
            this.alarmVibratorService.startAlarm();
            this._isSirenPlaying.setValue((Object)true);
            this._emergencySession.setValue((Object)EmergencySession.copy$default((EmergencySession)this._emergencySession.getValue(), null, null, 0, null, null, null, 0, 0L, null, false, false, false, 3583, null));
            job = BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
                int label;
                final /* synthetic */ GuardianViewModel this$0;
                {
                    this.this$0 = $receiver;
                    super(2, $completion);
                }

                /*
                 * Enabled force condition propagation
                 * Lifted jumps to return sites
                 */
                public final Object invokeSuspend(Object $result) {
                    Object object = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                    switch (this.label) {
                        case 0: {
                            ResultKt.throwOnFailure((Object)$result);
                            this.label = 1;
                            Object object2 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Siren alarm sounding!"), (Continuation)this);
                            if (object2 != object) return Unit.INSTANCE;
                            return object;
                        }
                        case 1: {
                            ResultKt.throwOnFailure((Object)$result);
                            Object object2 = $result;
                            return Unit.INSTANCE;
                        }
                    }
                    throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                }

                public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                    return (Continuation)new /* invalid duplicate definition of identical inner class */;
                }

                public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                    return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
                }
            }), (int)3, null);
        }
    }

    @NotNull
    public final StateFlow<Boolean> getCriticalAlarmsEnabled() {
        return this.criticalAlarmsEnabled;
    }

    public final void setVoiceSosEnabled(boolean enabled) {
        this._voiceSosEnabled.setValue((Object)enabled);
        try {
            ComponentName componentName;
            this.getApplication().getSharedPreferences("smart_sos_settings", 0).edit().putBoolean("voice_sos_enabled", enabled).apply();
            Intent intent = new Intent((Context)this.getApplication(), VoiceSosForegroundService.class);
            if (enabled) {
                componentName = Build.VERSION.SDK_INT >= 26 ? this.getApplication().startForegroundService(intent) : this.getApplication().startService(intent);
            } else {
                intent.setAction("STOP");
                componentName = this.getApplication().startService(intent);
            }
            Comparable cfr_ignored_0 = (Comparable)componentName;
        }
        catch (Exception e) {
            Log.e((String)"GuardianViewModel", (String)("Failed to save/start voice_sos_enabled: " + e.getMessage()));
        }
    }

    public final void setVoiceSosPhrase(@NotNull String phrase) {
        Intrinsics.checkNotNullParameter((Object)phrase, (String)"phrase");
        this._voiceSosPhrase.setValue((Object)phrase);
        this.databaseService.saveUserSetting("voice_sos_phrase", phrase);
    }

    public final void setCriticalAlarmsEnabled(boolean enabled) {
        this._criticalAlarmsEnabled.setValue((Object)enabled);
        this.databaseService.saveUserSetting("critical_alarms_enabled", enabled);
    }

    @NotNull
    public final StateFlow<Boolean> getArrivalAlertsEnabled() {
        return this.arrivalAlertsEnabled;
    }

    public final void setArrivalAlertsEnabled(boolean enabled) {
        this._arrivalAlertsEnabled.setValue((Object)enabled);
    }

    @NotNull
    public final StateFlow<Boolean> getDeviceStatusNotificationsEnabled() {
        return this.deviceStatusNotificationsEnabled;
    }

    public final void setDeviceStatusNotificationsEnabled(boolean enabled) {
        this._deviceStatusNotificationsEnabled.setValue((Object)enabled);
    }

    @NotNull
    public final StateFlow<String> getLocationSharingInterval() {
        return this.locationSharingInterval;
    }

    public final void setLocationSharingInterval(@NotNull String interval) {
        Intrinsics.checkNotNullParameter((Object)interval, (String)"interval");
        this._locationSharingInterval.setValue((Object)interval);
    }

    @NotNull
    public final StateFlow<Boolean> getBackgroundLocationEnabled() {
        return this.backgroundLocationEnabled;
    }

    public final void setBackgroundLocationEnabled(boolean enabled) {
        this._backgroundLocationEnabled.setValue((Object)enabled);
    }

    @NotNull
    public final StateFlow<Boolean> getTelemetrySharingEnabled() {
        return this.telemetrySharingEnabled;
    }

    public final void setTelemetrySharingEnabled(boolean enabled) {
        this._telemetrySharingEnabled.setValue((Object)enabled);
    }

    @NotNull
    public final StateFlow<Boolean> getBiometricEnabled() {
        return this.biometricEnabled;
    }

    public final void setBiometricEnabled(boolean enabled) {
        this._biometricEnabled.setValue((Object)enabled);
    }

    @NotNull
    public final StateFlow<Boolean> getAppLockPinEnabled() {
        return this.appLockPinEnabled;
    }

    @NotNull
    public final StateFlow<String> getAppLockPin() {
        return this.appLockPin;
    }

    public final void setAppLockPin(@NotNull String pin, boolean enabled) {
        Intrinsics.checkNotNullParameter((Object)pin, (String)"pin");
        this._appLockPin.setValue((Object)pin);
        this._appLockPinEnabled.setValue((Object)enabled);
    }

    @NotNull
    public final StateFlow<String> getEmergencyPin() {
        return this.emergencyPin;
    }

    public final void setEmergencyPin(@NotNull String pin) {
        Intrinsics.checkNotNullParameter((Object)pin, (String)"pin");
        this.securityService.saveEmergencyPin(pin);
        this._emergencyPin.setValue((Object)pin);
    }

    @NotNull
    public final StateFlow<Boolean> isBackupRunning() {
        return this.isBackupRunning;
    }

    @NotNull
    public final StateFlow<String> getLastBackupTime() {
        return this.lastBackupTime;
    }

    public final void runBackup() {
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            {
                this.this$0 = $receiver;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var2_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        GuardianViewModel.access$get_isBackupRunning$p(this.this$0).setValue((Object)Boxing.boxBoolean((boolean)true));
                        this.label = 1;
                        v0 = DelayKt.delay((long)3000L, (Continuation)((Continuation)this));
                        if (v0 == var2_2) {
                            return var2_2;
                        }
                        ** GOTO lbl14
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl14:
                        // 2 sources

                        v1 = GuardianViewModel.access$get_lastBackupTime$p(this.this$0);
                        v2 = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date());
                        Intrinsics.checkNotNullExpressionValue((Object)v2, (String)"format(...)");
                        v1.setValue((Object)v2);
                        GuardianViewModel.access$get_isBackupRunning$p(this.this$0).setValue((Object)Boxing.boxBoolean((boolean)false));
                        this.label = 2;
                        v3 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Cloud Backup Completed Successfully!"), (Continuation)this);
                        if (v3 == var2_2) {
                            return var2_2;
                        }
                        ** GOTO lbl27
                    }
                    case 2: {
                        ResultKt.throwOnFailure((Object)$result);
                        v3 = $result;
lbl27:
                        // 2 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void runRestore() {
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            {
                this.this$0 = $receiver;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var2_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        GuardianViewModel.access$get_isBackupRunning$p(this.this$0).setValue((Object)Boxing.boxBoolean((boolean)true));
                        this.label = 1;
                        v0 = DelayKt.delay((long)3000L, (Continuation)((Continuation)this));
                        if (v0 == var2_2) {
                            return var2_2;
                        }
                        ** GOTO lbl14
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl14:
                        // 2 sources

                        GuardianViewModel.access$get_isBackupRunning$p(this.this$0).setValue((Object)Boxing.boxBoolean((boolean)false));
                        this.label = 2;
                        v1 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Local Database Restored from Cloud!"), (Continuation)this);
                        if (v1 == var2_2) {
                            return var2_2;
                        }
                        ** GOTO lbl23
                    }
                    case 2: {
                        ResultKt.throwOnFailure((Object)$result);
                        v1 = $result;
lbl23:
                        // 2 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void changePassword(@NotNull String old, @NotNull String string, @NotNull Function1<? super Boolean, Unit> callback) {
        Intrinsics.checkNotNullParameter((Object)old, (String)"old");
        Intrinsics.checkNotNullParameter((Object)string, (String)"new");
        Intrinsics.checkNotNullParameter(callback, (String)"callback");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(old, string, callback, this, null){
            int label;
            final /* synthetic */ String $old;
            final /* synthetic */ String $new;
            final /* synthetic */ Function1<Boolean, Unit> $callback;
            final /* synthetic */ GuardianViewModel this$0;
            {
                this.$old = $old;
                this.$new = $new;
                this.$callback = $callback;
                this.this$0 = $receiver;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                block7: {
                    var2_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                    switch (this.label) {
                        case 0: {
                            ResultKt.throwOnFailure((Object)$result);
                            this.label = 1;
                            v0 = DelayKt.delay((long)1000L, (Continuation)((Continuation)this));
                            if (v0 == var2_2) {
                                return var2_2;
                            }
                            ** GOTO lbl13
                        }
                        case 1: {
                            ResultKt.throwOnFailure((Object)$result);
                            v0 = $result;
lbl13:
                            // 2 sources

                            if (this.$old.length() < 4 || this.$new.length() < 4) break;
                            this.$callback.invoke((Object)Boxing.boxBoolean((boolean)true));
                            this.label = 2;
                            v1 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Password Changed Successfully!"), (Continuation)this);
                            if (v1 == var2_2) {
                                return var2_2;
                            }
                            break block7;
                        }
                        case 2: {
                            ResultKt.throwOnFailure((Object)$result);
                            v1 = $result;
                            break block7;
                        }
                    }
                    this.$callback.invoke((Object)Boxing.boxBoolean((boolean)false));
                }
                return Unit.INSTANCE;
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void deleteAccount() {
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            {
                this.this$0 = $receiver;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var2_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.this$0.getAuthService().logout();
                        this.label = 1;
                        v0 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Account permanently deleted."), (Continuation)this);
                        if (v0 == var2_2) {
                            return var2_2;
                        }
                        ** GOTO lbl14
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl14:
                        // 2 sources

                        this.label = 2;
                        v1 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)UiEvent.NavigateToLogin.INSTANCE, (Continuation)this);
                        if (v1 == var2_2) {
                            return var2_2;
                        }
                        ** GOTO lbl22
                    }
                    case 2: {
                        ResultKt.throwOnFailure((Object)$result);
                        v1 = $result;
lbl22:
                        // 2 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    @NotNull
    public final SharedFlow<UiEvent> getUiEvents() {
        return this.uiEvents;
    }

    @NotNull
    public final StateFlow<PermissionsState> getPermissionsState() {
        return this.permissionsState;
    }

    public final void refreshPermissions(@NotNull Context context) {
        boolean contacts;
        boolean location;
        Intrinsics.checkNotNullParameter((Object)context, (String)"context");
        boolean bl = location = ContextCompat.checkSelfPermission((Context)context, (String)"android.permission.ACCESS_FINE_LOCATION") == 0;
        boolean background = Build.VERSION.SDK_INT >= 29 ? ContextCompat.checkSelfPermission((Context)context, (String)"android.permission.ACCESS_BACKGROUND_LOCATION") == 0 : true;
        boolean calls = ContextCompat.checkSelfPermission((Context)context, (String)"android.permission.CALL_PHONE") == 0;
        boolean sms = ContextCompat.checkSelfPermission((Context)context, (String)"android.permission.SEND_SMS") == 0;
        boolean bl2 = contacts = ContextCompat.checkSelfPermission((Context)context, (String)"android.permission.READ_CONTACTS") == 0;
        boolean notifs = Build.VERSION.SDK_INT >= 33 ? ContextCompat.checkSelfPermission((Context)context, (String)"android.permission.POST_NOTIFICATIONS") == 0 : true;
        boolean audio = ContextCompat.checkSelfPermission((Context)context, (String)"android.permission.RECORD_AUDIO") == 0;
        boolean overlay = Settings.canDrawOverlays((Context)context);
        this._permissionsState.setValue((Object)new PermissionsState(location, background, calls, sms, contacts, notifs, audio, overlay));
    }

    public final boolean isDemoMode() {
        return this.authService.isDemoMode();
    }

    public final void loginUser(@NotNull String email, @NotNull String pass) {
        Intrinsics.checkNotNullParameter((Object)email, (String)"email");
        Intrinsics.checkNotNullParameter((Object)pass, (String)"pass");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, email, pass, null){
            Object L$0;
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            final /* synthetic */ String $email;
            final /* synthetic */ String $pass;
            {
                this.this$0 = $receiver;
                this.$email = $email;
                this.$pass = $pass;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var3_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        v0 = this.this$0.getAuthService().login(this.$email, this.$pass, (Continuation<? super Unit>)((Continuation)this));
                        if (v0 == var3_2) {
                            return var3_2;
                        }
                        ** GOTO lbl13
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl13:
                        // 2 sources

                        if (!((currentState = (AuthState)this.this$0.getAuthService().getAuthState().getValue()) instanceof AuthState.Success)) break;
                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)currentState);
                        this.label = 2;
                        v1 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Welcome back, " + ((AuthState.Success)currentState).getUser().getName() + "!"), (Continuation)this);
                        if (v1 == var3_2) {
                            return var3_2;
                        }
                        ** GOTO lbl24
                    }
                    case 2: {
                        currentState = (AuthState)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v1 = $result;
lbl24:
                        // 2 sources

                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)currentState);
                        this.label = 3;
                        v2 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)UiEvent.NavigateToHome.INSTANCE, (Continuation)this);
                        if (v2 == var3_2) {
                            return var3_2;
                        }
                        ** GOTO lbl46
                    }
                    case 3: {
                        currentState = (AuthState)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v2 = $result;
                        ** GOTO lbl46
                    }
                }
                if (currentState instanceof AuthState.Error) {
                    this.L$0 = SpillingKt.nullOutSpilledVariable((Object)currentState);
                    this.label = 4;
                    v3 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast(((AuthState.Error)currentState).getMessage()), (Continuation)this);
                    if (v3 == var3_2) {
                        return var3_2;
                    }
                }
                ** GOTO lbl46
                {
                    case 4: {
                        currentState = (AuthState)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v3 = $result;
lbl46:
                        // 4 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void registerUser(@NotNull String name, @NotNull String email, @NotNull String phone, @NotNull String medical, @NotNull String contactName, @NotNull String contactPhone, @NotNull String pass) {
        Intrinsics.checkNotNullParameter((Object)name, (String)"name");
        Intrinsics.checkNotNullParameter((Object)email, (String)"email");
        Intrinsics.checkNotNullParameter((Object)phone, (String)"phone");
        Intrinsics.checkNotNullParameter((Object)medical, (String)"medical");
        Intrinsics.checkNotNullParameter((Object)contactName, (String)"contactName");
        Intrinsics.checkNotNullParameter((Object)contactPhone, (String)"contactPhone");
        Intrinsics.checkNotNullParameter((Object)pass, (String)"pass");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(name, email, phone, medical, contactName, contactPhone, this, pass, null){
            Object L$0;
            Object L$1;
            int label;
            final /* synthetic */ String $name;
            final /* synthetic */ String $email;
            final /* synthetic */ String $phone;
            final /* synthetic */ String $medical;
            final /* synthetic */ String $contactName;
            final /* synthetic */ String $contactPhone;
            final /* synthetic */ GuardianViewModel this$0;
            final /* synthetic */ String $pass;
            {
                this.$name = $name;
                this.$email = $email;
                this.$phone = $phone;
                this.$medical = $medical;
                this.$contactName = $contactName;
                this.$contactPhone = $contactPhone;
                this.this$0 = $receiver;
                this.$pass = $pass;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var4_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        newUser = new User(null, this.$name, this.$email, this.$phone, this.$medical, this.$contactName, this.$contactPhone, "User", 0L, null, null, null, null, null, 16129, null);
                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)newUser);
                        this.label = 1;
                        v0 = this.this$0.getAuthService().register(newUser, this.$pass, (Continuation<? super Unit>)((Continuation)this));
                        if (v0 == var4_2) {
                            return var4_2;
                        }
                        ** GOTO lbl16
                    }
                    case 1: {
                        newUser = (User)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl16:
                        // 2 sources

                        if (!((currentState = (AuthState)this.this$0.getAuthService().getAuthState().getValue()) instanceof AuthState.Success)) break;
                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)newUser);
                        this.L$1 = SpillingKt.nullOutSpilledVariable((Object)currentState);
                        this.label = 2;
                        v1 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Account created successfully!"), (Continuation)this);
                        if (v1 == var4_2) {
                            return var4_2;
                        }
                        ** GOTO lbl29
                    }
                    case 2: {
                        currentState = (AuthState)this.L$1;
                        newUser = (User)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v1 = $result;
lbl29:
                        // 2 sources

                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)newUser);
                        this.L$1 = SpillingKt.nullOutSpilledVariable((Object)currentState);
                        this.label = 3;
                        v2 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)UiEvent.NavigateToHome.INSTANCE, (Continuation)this);
                        if (v2 == var4_2) {
                            return var4_2;
                        }
                        ** GOTO lbl55
                    }
                    case 3: {
                        currentState = (AuthState)this.L$1;
                        newUser = (User)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v2 = $result;
                        ** GOTO lbl55
                    }
                }
                if (currentState instanceof AuthState.Error) {
                    this.L$0 = SpillingKt.nullOutSpilledVariable((Object)newUser);
                    this.L$1 = SpillingKt.nullOutSpilledVariable((Object)currentState);
                    this.label = 4;
                    v3 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast(((AuthState.Error)currentState).getMessage()), (Continuation)this);
                    if (v3 == var4_2) {
                        return var4_2;
                    }
                }
                ** GOTO lbl55
                {
                    case 4: {
                        currentState = (AuthState)this.L$1;
                        newUser = (User)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v3 = $result;
lbl55:
                        // 4 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void resetPassword(@NotNull String email) {
        Intrinsics.checkNotNullParameter((Object)email, (String)"email");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, email, null){
            boolean Z$0;
            Object L$0;
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            final /* synthetic */ String $email;
            {
                this.this$0 = $receiver;
                this.$email = $email;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                block13: {
                    var4_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                    switch (this.label) {
                        case 0: {
                            ResultKt.throwOnFailure((Object)$result);
                            this.label = 1;
                            v0 = this.this$0.getAuthService().resetPassword(this.$email, (Continuation<? super Boolean>)((Continuation)this));
                            if (v0 == var4_2) {
                                return var4_2;
                            }
                            ** GOTO lbl13
                        }
                        case 1: {
                            ResultKt.throwOnFailure((Object)$result);
                            v0 = $result;
lbl13:
                            // 2 sources

                            if (!(success = ((Boolean)v0).booleanValue())) break;
                            this.Z$0 = success;
                            this.label = 2;
                            v1 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Password reset link dispatched to " + this.$email + "!"), (Continuation)this);
                            if (v1 == var4_2) {
                                return var4_2;
                            }
                            ** GOTO lbl24
                        }
                        case 2: {
                            success = this.Z$0;
                            ResultKt.throwOnFailure((Object)$result);
                            v1 = $result;
lbl24:
                            // 2 sources

                            this.Z$0 = success;
                            this.label = 3;
                            v2 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)UiEvent.NavigateToLogin.INSTANCE, (Continuation)this);
                            if (v2 == var4_2) {
                                return var4_2;
                            }
                            ** GOTO lbl63
                        }
                        case 3: {
                            success = this.Z$0;
                            ResultKt.throwOnFailure((Object)$result);
                            v2 = $result;
                            ** GOTO lbl63
                        }
                    }
                    currentState = (AuthState)this.this$0.getAuthService().getAuthState().getValue();
                    if (!(currentState instanceof AuthState.Error)) break block13;
                    this.L$0 = SpillingKt.nullOutSpilledVariable((Object)currentState);
                    this.Z$0 = success;
                    this.label = 4;
                    v3 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast(((AuthState.Error)currentState).getMessage()), (Continuation)this);
                    if (v3 == var4_2) {
                        return var4_2;
                    }
                    ** GOTO lbl63
                    {
                        case 4: {
                            success = this.Z$0;
                            currentState = (AuthState)this.L$0;
                            ResultKt.throwOnFailure((Object)$result);
                            v3 = $result;
                            ** GOTO lbl63
                        }
                    }
                }
                this.L$0 = SpillingKt.nullOutSpilledVariable((Object)currentState);
                this.Z$0 = success;
                this.label = 5;
                v4 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Failed to reset password."), (Continuation)this);
                if (v4 == var4_2) {
                    return var4_2;
                }
                ** GOTO lbl63
                {
                    case 5: {
                        success = this.Z$0;
                        currentState = (AuthState)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v4 = $result;
lbl63:
                        // 6 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void logout() {
        this.authService.logout();
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            {
                this.this$0 = $receiver;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var2_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        v0 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Logged out successfully."), (Continuation)this);
                        if (v0 == var2_2) {
                            return var2_2;
                        }
                        ** GOTO lbl13
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl13:
                        // 2 sources

                        this.label = 2;
                        v1 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)UiEvent.NavigateToLogin.INSTANCE, (Continuation)this);
                        if (v1 == var2_2) {
                            return var2_2;
                        }
                        ** GOTO lbl21
                    }
                    case 2: {
                        ResultKt.throwOnFailure((Object)$result);
                        v1 = $result;
lbl21:
                        // 2 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void updateUserProfile(@NotNull User updatedUser) {
        Intrinsics.checkNotNullParameter((Object)updatedUser, (String)"updatedUser");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, updatedUser, null){
            Object L$0;
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            final /* synthetic */ User $updatedUser;
            {
                this.this$0 = $receiver;
                this.$updatedUser = $updatedUser;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var3_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        v0 = this.this$0.getAuthService().updateProfile(this.$updatedUser, (Continuation<? super Unit>)((Continuation)this));
                        if (v0 == var3_2) {
                            return var3_2;
                        }
                        ** GOTO lbl13
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl13:
                        // 2 sources

                        if (!((currentState = (AuthState)this.this$0.getAuthService().getAuthState().getValue()) instanceof AuthState.Success)) break;
                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)currentState);
                        this.label = 2;
                        v1 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Profile successfully updated!"), (Continuation)this);
                        if (v1 == var3_2) {
                            return var3_2;
                        }
                        ** GOTO lbl36
                    }
                    case 2: {
                        currentState = (AuthState)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v1 = $result;
                        ** GOTO lbl36
                    }
                }
                if (currentState instanceof AuthState.Error) {
                    this.L$0 = SpillingKt.nullOutSpilledVariable((Object)currentState);
                    this.label = 3;
                    v2 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast(((AuthState.Error)currentState).getMessage()), (Continuation)this);
                    if (v2 == var3_2) {
                        return var3_2;
                    }
                }
                ** GOTO lbl36
                {
                    case 3: {
                        currentState = (AuthState)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v2 = $result;
lbl36:
                        // 4 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final boolean checkSystemReadiness() {
        boolean isConnected;
        NetworkCapabilities networkCapabilities;
        Application context = this.getApplication();
        boolean isReady = true;
        Object object = context.getSystemService("location");
        Intrinsics.checkNotNull((Object)object, (String)"null cannot be cast to non-null type android.location.LocationManager");
        LocationManager locationManager = (LocationManager)object;
        if (!locationManager.isProviderEnabled("gps")) {
            BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
                int label;
                final /* synthetic */ GuardianViewModel this$0;
                {
                    this.this$0 = $receiver;
                    super(2, $completion);
                }

                /*
                 * Enabled force condition propagation
                 * Lifted jumps to return sites
                 */
                public final Object invokeSuspend(Object $result) {
                    Object object = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                    switch (this.label) {
                        case 0: {
                            ResultKt.throwOnFailure((Object)$result);
                            this.label = 1;
                            Object object2 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("WARNING: GPS is disabled! Location cannot be tracked."), (Continuation)this);
                            if (object2 != object) return Unit.INSTANCE;
                            return object;
                        }
                        case 1: {
                            ResultKt.throwOnFailure((Object)$result);
                            Object object2 = $result;
                            return Unit.INSTANCE;
                        }
                    }
                    throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                }

                public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                    return (Continuation)new /* invalid duplicate definition of identical inner class */;
                }

                public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                    return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
                }
            }), (int)3, null);
            isReady = false;
        }
        Object object2 = context.getSystemService("connectivity");
        Intrinsics.checkNotNull((Object)object2, (String)"null cannot be cast to non-null type android.net.ConnectivityManager");
        ConnectivityManager connectivityManager = (ConnectivityManager)object2;
        Network activeNetwork = connectivityManager.getActiveNetwork();
        NetworkCapabilities networkCapabilities2 = networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        boolean bl = networkCapabilities2 != null ? networkCapabilities2.hasCapability(12) : (isConnected = false);
        if (!isConnected) {
            BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
                int label;
                final /* synthetic */ GuardianViewModel this$0;
                {
                    this.this$0 = $receiver;
                    super(2, $completion);
                }

                /*
                 * Enabled force condition propagation
                 * Lifted jumps to return sites
                 */
                public final Object invokeSuspend(Object $result) {
                    Object object = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                    switch (this.label) {
                        case 0: {
                            ResultKt.throwOnFailure((Object)$result);
                            this.label = 1;
                            Object object2 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("WARNING: No Internet! Remote alerts may fail."), (Continuation)this);
                            if (object2 != object) return Unit.INSTANCE;
                            return object;
                        }
                        case 1: {
                            ResultKt.throwOnFailure((Object)$result);
                            Object object2 = $result;
                            return Unit.INSTANCE;
                        }
                    }
                    throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                }

                public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                    return (Continuation)new /* invalid duplicate definition of identical inner class */;
                }

                public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                    return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
                }
            }), (int)3, null);
            isReady = false;
        }
        Object object3 = context.getSystemService("notification");
        Intrinsics.checkNotNull((Object)object3, (String)"null cannot be cast to non-null type android.app.NotificationManager");
        NotificationManager notificationManager = (NotificationManager)object3;
        if (!notificationManager.areNotificationsEnabled()) {
            BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
                int label;
                final /* synthetic */ GuardianViewModel this$0;
                {
                    this.this$0 = $receiver;
                    super(2, $completion);
                }

                /*
                 * Enabled force condition propagation
                 * Lifted jumps to return sites
                 */
                public final Object invokeSuspend(Object $result) {
                    Object object = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                    switch (this.label) {
                        case 0: {
                            ResultKt.throwOnFailure((Object)$result);
                            this.label = 1;
                            Object object2 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("WARNING: Notifications are disabled!"), (Continuation)this);
                            if (object2 != object) return Unit.INSTANCE;
                            return object;
                        }
                        case 1: {
                            ResultKt.throwOnFailure((Object)$result);
                            Object object2 = $result;
                            return Unit.INSTANCE;
                        }
                    }
                    throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                }

                public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                    return (Continuation)new /* invalid duplicate definition of identical inner class */;
                }

                public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                    return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
                }
            }), (int)3, null);
            isReady = false;
        }
        return isReady;
    }

    private final TrustedPlace getMatchedTrustedPlace(double lat, double lng) {
        float[] results = new float[1];
        for (TrustedPlace place : (List)this.trustedPlacesService.getTrustedPlaces().getValue()) {
            Location.distanceBetween((double)lat, (double)lng, (double)place.getLatitude(), (double)place.getLongitude(), (float[])results);
            if (!((double)results[0] <= place.getRadius())) continue;
            return place;
        }
        return null;
    }

    /*
     * Unable to fully structure code
     */
    private final Object initiateEmergencySequence(String triggerSource, String deviceId, Double lat, Double lng, Float accuracy, Double altitude, Float speed, Float bearing, Continuation<? super EmergencyModel> $completion) {
        this.checkSystemReadiness();
        var12_10 = this.authState.getValue();
        v0 = var12_10 instanceof AuthState.Success != false ? (AuthState.Success)var12_10 : null;
        user = v0 != null ? v0.getUser() : null;
        v1 = user;
        if (v1 == null || (v1 = v1.getUid()) == null) {
            v1 = userId = "user-101";
        }
        if ((v2 = user) == null || (v2 = v2.getName()) == null) {
            v2 = userName = "Marcus Vance";
        }
        if ((v3 = user) == null || (v3 = v3.getPhone()) == null) {
            v3 = "+1-555-0143";
        }
        userPhone = v3;
        v4 = lat;
        v5 = lng;
        matchedPlace = this.getMatchedTrustedPlace(v4 != null ? v4.doubleValue() : ((UserLocation)this.locationService.getCurrentLocation().getValue()).getLatitude(), v5 != null ? v5.doubleValue() : ((UserLocation)this.locationService.getCurrentLocation().getValue()).getLongitude());
        if (!((Boolean)this.sosSoundEnabled.getValue()).booleanValue()) ** GOTO lbl-1000
        v6 = matchedPlace;
        if (!(v6 != null ? v6.getReduceNotificationSound() : false)) {
            v7 = true;
        } else lbl-1000:
        // 2 sources

        {
            v7 = false;
        }
        this._isSirenPlaying.setValue((Object)Boxing.boxBoolean((boolean)v7));
        return this.emergencyProvider.initiateEmergency((String)userId, (String)userName, (String)userPhone, triggerSource, deviceId, lat, lng, accuracy, altitude, speed, bearing, $completion);
    }

    static /* synthetic */ Object initiateEmergencySequence$default(GuardianViewModel guardianViewModel, String string, String string2, Double d, Double d2, Float f, Double d3, Float f2, Float f3, Continuation continuation, int n, Object object) {
        if ((n & 4) != 0) {
            d = null;
        }
        if ((n & 8) != 0) {
            d2 = null;
        }
        if ((n & 0x10) != 0) {
            f = null;
        }
        if ((n & 0x20) != 0) {
            d3 = null;
        }
        if ((n & 0x40) != 0) {
            f2 = null;
        }
        if ((n & 0x80) != 0) {
            f3 = null;
        }
        return guardianViewModel.initiateEmergencySequence(string, string2, d, d2, f, d3, f2, f3, (Continuation<? super EmergencyModel>)continuation);
    }

    public final void triggerTimerSOS() {
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            {
                this.this$0 = $receiver;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var2_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        v0 = GuardianViewModel.initiateEmergencySequence$default(this.this$0, "SAFETY_TIMER_EXPIRED", "MOBILE-APP-TIMER", null, null, null, null, null, null, (Continuation)this, 252, null);
                        if (v0 == var2_2) {
                            return var2_2;
                        }
                        ** GOTO lbl13
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl13:
                        // 2 sources

                        this.label = 2;
                        v1 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("\ud83d\udea8 SAFETY TIMER EXPIRED: AUTOMATIC SOS DISPATCHED!"), (Continuation)this);
                        if (v1 == var2_2) {
                            return var2_2;
                        }
                        ** GOTO lbl21
                    }
                    case 2: {
                        ResultKt.throwOnFailure((Object)$result);
                        v1 = $result;
lbl21:
                        // 2 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void triggerManualSOS(double lat, double lng) {
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            {
                this.this$0 = $receiver;
                super(2, $completion);
            }

            /*
             * Enabled force condition propagation
             * Lifted jumps to return sites
             */
            public final Object invokeSuspend(Object $result) {
                Object object = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        if (GuardianViewModel.access$get_sosWorkflowState$p(this.this$0).getValue() != SosWorkflowState.IDLE && GuardianViewModel.access$get_sosWorkflowState$p(this.this$0).getValue() != SosWorkflowState.COMPLETED) {
                            return Unit.INSTANCE;
                        }
                        if (!this.this$0.getEmergencyService().isEmergencyActive()) break;
                        EmergencyModel emergencyModel = (EmergencyModel)this.this$0.getEmergencyService().getActiveEmergency().getValue();
                        if (emergencyModel != null) {
                            EmergencyModel emergencyModel2 = emergencyModel;
                            GuardianViewModel guardianViewModel = this.this$0;
                            EmergencyModel emergencyModel3 = emergencyModel2;
                            boolean bl = false;
                            guardianViewModel.getEmergencyService().notifyEmergencyContacts(emergencyModel3, true);
                        }
                        this.label = 1;
                        Object object2 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("ALERT TRANSMITTED: Contacts Notified Again!"), (Continuation)this);
                        if (object2 != object) return Unit.INSTANCE;
                        return object;
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        Object object2 = $result;
                        return Unit.INSTANCE;
                    }
                }
                GuardianViewModel.access$get_sosWorkflowState$p(this.this$0).setValue((Object)((Object)SosWorkflowState.IDLE));
                this.label = 2;
                Object object3 = GuardianViewModel.initiateEmergencySequence$default(this.this$0, "MANUAL", "MOBILE-APP-SOS", null, null, null, null, null, null, (Continuation)this, 252, null);
                if (object3 != object) return Unit.INSTANCE;
                return object;
                {
                    case 2: {
                        ResultKt.throwOnFailure((Object)$result);
                        object3 = $result;
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public static /* synthetic */ void triggerManualSOS$default(GuardianViewModel guardianViewModel, double d, double d2, int n, Object object) {
        if ((n & 1) != 0) {
            d = 37.7749;
        }
        if ((n & 2) != 0) {
            d2 = -122.4194;
        }
        guardianViewModel.triggerManualSOS(d, d2);
    }

    public final void triggerFallDetectedSOS() {
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
            Object L$0;
            Object L$1;
            Object L$2;
            Object L$3;
            Object L$4;
            int I$0;
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            {
                this.this$0 = $receiver;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var11_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        hwGps = (HardwareGpsLocation)this.this$0.getDeviceService().getBleManager().getLatestHardwareGpsLocation().getValue();
                        v0 = isGpsValid = this.this$0.getDeviceService().getBleManager().getHardwareGpsState().getValue() instanceof HardwareGpsState.ValidLocation != false && hwGps != null ? 1 : 0;
                        if (isGpsValid == 0 || hwGps == null) break;
                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)hwGps);
                        this.I$0 = isGpsValid;
                        this.label = 1;
                        v1 = GuardianViewModel.initiateEmergencySequence$default(this.this$0, "FALL_DETECTED", "ESP32-SOS-BAND-81F4", Boxing.boxDouble((double)hwGps.getLatitude()), Boxing.boxDouble((double)hwGps.getLongitude()), Boxing.boxFloat((float)3.0f), null, null, null, (Continuation)this, 224, null);
                        if (v1 == var11_2) {
                            return var11_2;
                        }
                        ** GOTO lbl20
                    }
                    case 1: {
                        isGpsValid = this.I$0;
                        hwGps = (HardwareGpsLocation)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v1 = $result;
lbl20:
                        // 2 sources

                        v2 = (EmergencyModel)v1;
                        ** GOTO lbl35
                    }
                }
                this.L$0 = SpillingKt.nullOutSpilledVariable((Object)hwGps);
                this.I$0 = isGpsValid;
                this.label = 2;
                v3 = GuardianViewModel.initiateEmergencySequence$default(this.this$0, "FALL_DETECTED", "ESP32-SOS-BAND-81F4", null, null, null, null, null, null, (Continuation)this, 252, null);
                if (v3 == var11_2) {
                    return var11_2;
                }
                ** GOTO lbl34
                {
                    case 2: {
                        isGpsValid = this.I$0;
                        hwGps = (HardwareGpsLocation)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v3 = $result;
lbl34:
                        // 2 sources

                        v2 = (EmergencyModel)v3;
lbl35:
                        // 2 sources

                        model = v2;
                        reading = (Mpu6050Reading)this.this$0.getDeviceService().getBleManager().getLatestMpuReading().getValue();
                        if (reading != null) {
                            var8_11 = Locale.US;
                            var9_12 = "%.2fg";
                            var10_13 = new Object[]{Boxing.boxFloat((float)reading.getAccelerationMagnitudeG())};
                            v4 = String.format((Locale)var8_11, var9_12, Arrays.copyOf(var10_13, var10_13.length));
                            v5 = v4;
                            Intrinsics.checkNotNullExpressionValue((Object)v4, (String)"format(...)");
                        } else {
                            v5 = "4.1G";
                        }
                        magStr = v5;
                        var8_11 = new AITimelineEvent[]{new AITimelineEvent("10:44:00 AM", "Impact Shock", "MPU6050 accelerometer spike (" + magStr + ") logged.", "\ud83d\udca5"), new AITimelineEvent("10:44:05 AM", "Countdown Commenced", "Wearer unresponsive. 15-second countdown started.", "\u23f1\ufe0f"), new AITimelineEvent("10:44:20 AM", "Auto SOS Dispatch", "No cancel received. Dispatched emergency broadcast.", "\ud83d\udea8")};
                        analysis = new AIAnalysisModel(null, model.getEmergencyId(), 98, 2, "CRITICAL_ACCELERATION_SPIKE_FOLLOWED_BY_HORIZONTAL_AXIS_SHIFT", "SUDDEN FALL DETECTED (STATIC LAYING)", "CRITICAL", "ALERT ALL PRIMARY FAMILY CONTACTS AND LAUNCH COUNTY DISPATCH CODES", null, CollectionsKt.listOf((Object[])var8_11), 0L, 1281, null);
                        this.this$0.getAiService().addAnalysisLog(analysis);
                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)hwGps);
                        this.L$1 = SpillingKt.nullOutSpilledVariable((Object)model);
                        this.L$2 = SpillingKt.nullOutSpilledVariable((Object)reading);
                        this.L$3 = SpillingKt.nullOutSpilledVariable((Object)magStr);
                        this.L$4 = SpillingKt.nullOutSpilledVariable((Object)analysis);
                        this.I$0 = isGpsValid;
                        this.label = 3;
                        v6 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("\ud83d\udea8 FALL DETECTED: AUTOMATIC SOS DISPATCHED!"), (Continuation)this);
                        if (v6 == var11_2) {
                            return var11_2;
                        }
                        ** GOTO lbl71
                    }
                    case 3: {
                        isGpsValid = this.I$0;
                        analysis = (AIAnalysisModel)this.L$4;
                        magStr = (String)this.L$3;
                        reading = (Mpu6050Reading)this.L$2;
                        model = (EmergencyModel)this.L$1;
                        hwGps = (HardwareGpsLocation)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v6 = $result;
lbl71:
                        // 2 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void triggerVoiceSOS(@NotNull String matchedPhrase, int confidence) {
        Intrinsics.checkNotNullParameter((Object)matchedPhrase, (String)"matchedPhrase");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, confidence, matchedPhrase, null){
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            final /* synthetic */ int $confidence;
            final /* synthetic */ String $matchedPhrase;
            {
                this.this$0 = $receiver;
                this.$confidence = $confidence;
                this.$matchedPhrase = $matchedPhrase;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var5_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        v0 = GuardianViewModel.initiateEmergencySequence$default(this.this$0, "VOICE_SOS", "MOBILE-VOICE-RECOGNIZE", null, null, null, null, null, null, (Continuation)this, 252, null);
                        if (v0 == var5_2) {
                            return var5_2;
                        }
                        ** GOTO lbl13
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl13:
                        // 2 sources

                        model = (EmergencyModel)v0;
                        var4_4 = new AITimelineEvent[]{new AITimelineEvent("10:44:00 AM", "Voice Alert Heard", "Acoustic sensor detected wake phrase \"" + this.$matchedPhrase + "\".", "\ud83c\udfa4"), new AITimelineEvent("10:44:02 AM", "Neural Match Lock", "Matched against offline template with " + this.$confidence + "% confidence.", "\ud83e\udde0"), new AITimelineEvent("10:44:03 AM", "SOS Dispatch", "Voice SOS emergency alert initiated.", "\ud83d\udea8")};
                        analysis = new AIAnalysisModel(null, model.getEmergencyId(), this.$confidence, 100 - this.$confidence, "AUDIO_FREQUENCY_WAVE_MATCH", "VOICE SOS ACTIVATION: \"" + this.$matchedPhrase + "\"", "CRITICAL", "WAKE WORD MATCHED DETECTOR. DISPATCH COGNITIVE RESPONSE AGENT.", null, CollectionsKt.listOf((Object[])var4_4), 0L, 1281, null);
                        this.this$0.getAiService().addAnalysisLog(analysis);
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void handleVoiceCommand(@NotNull VoiceCommand command, int confidence) {
        Intrinsics.checkNotNullParameter((Object)command, (String)"command");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(command, this, confidence, null){
            Object L$0;
            Object L$1;
            int label;
            final /* synthetic */ VoiceCommand $command;
            final /* synthetic */ GuardianViewModel this$0;
            final /* synthetic */ int $confidence;
            {
                this.$command = $command;
                this.this$0 = $receiver;
                this.$confidence = $confidence;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                block16: {
                    block18: {
                        block17: {
                            var6_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                            switch (this.label) {
                                case 0: {
                                    ResultKt.throwOnFailure((Object)$result);
                                    var2_3 = this.$command;
                                    if (!(var2_3 instanceof VoiceCommand.Sos)) break;
                                    this.this$0.triggerVoiceSOS(((VoiceCommand.Sos)this.$command).getMatchedPhrase(), this.$confidence);
                                    confirmationMsg = "\ud83d\udea8 Voice SOS: Countdown Initiated (\"" + ((VoiceCommand.Sos)this.$command).getMatchedPhrase() + "\")";
                                    GuardianViewModel.access$get_voiceCommandConfirmation$p(this.this$0).setValue((Object)confirmationMsg);
                                    this.L$0 = SpillingKt.nullOutSpilledVariable((Object)confirmationMsg);
                                    this.label = 1;
                                    v0 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast(confirmationMsg), (Continuation)this);
                                    if (v0 == var6_2) {
                                        return var6_2;
                                    }
                                    break block16;
                                }
                                case 1: {
                                    confirmationMsg = (String)this.L$0;
                                    ResultKt.throwOnFailure((Object)$result);
                                    v0 = $result;
                                    break block16;
                                }
                            }
                            if (!(var2_3 instanceof VoiceCommand.CancelSos)) break block17;
                            this.this$0.getAlarmVibratorService().stopAlarm();
                            this.this$0.getAlarmVibratorService().stopVibration();
                            GuardianViewModel.access$get_isSirenPlaying$p(this.this$0).setValue((Object)Boxing.boxBoolean((boolean)false));
                            if (this.this$0.getEmergencyService().isEmergencyActive()) {
                                this.this$0.getEmergencyService().markSafeAndClose();
                            }
                            Log.d((String)"SOS_ESP32", (String)"SOS CANCELLED");
                            this.this$0.getDeviceService().resetEsp32();
                            currentAlert = ((EmergencySession)GuardianViewModel.access$get_emergencySession$p(this.this$0).getValue()).getActiveAlert();
                            if (currentAlert != null) {
                                this.L$0 = SpillingKt.nullOutSpilledVariable((Object)currentAlert);
                                this.label = 2;
                                v1 = this.this$0.getDatabaseService().resolveSOS(currentAlert.getId(), "Voice Command", "Cancelled by voice command: " + ((VoiceCommand.CancelSos)this.$command).getMatchedPhrase(), (Continuation<? super Unit>)((Continuation)this));
                                if (v1 == var6_2) {
                                    return var6_2;
                                }
                            }
                            ** GOTO lbl42
                            {
                                case 2: {
                                    currentAlert = (Alert)this.L$0;
                                    ResultKt.throwOnFailure((Object)$result);
                                    v1 = $result;
lbl42:
                                    // 2 sources

                                    confirmationMsg = "\u2705 SOS Emergency cancelled via voice command: \"" + ((VoiceCommand.CancelSos)this.$command).getMatchedPhrase() + "\".";
                                    GuardianViewModel.access$get_voiceCommandConfirmation$p(this.this$0).setValue((Object)confirmationMsg);
                                    this.L$0 = SpillingKt.nullOutSpilledVariable((Object)currentAlert);
                                    this.L$1 = SpillingKt.nullOutSpilledVariable((Object)confirmationMsg);
                                    this.label = 3;
                                    v2 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast(confirmationMsg), (Continuation)this);
                                    if (v2 == var6_2) {
                                        return var6_2;
                                    }
                                    break block16;
                                }
                                case 3: {
                                    confirmationMsg = (String)this.L$1;
                                    currentAlert = (Alert)this.L$0;
                                    ResultKt.throwOnFailure((Object)$result);
                                    v2 = $result;
                                    break block16;
                                }
                            }
                        }
                        if (!(var2_3 instanceof VoiceCommand.TrackLocation)) break block18;
                        var5_14 = this.this$0.getAuthState().getValue();
                        v3 = var5_14 instanceof AuthState.Success != false ? (AuthState.Success)var5_14 : null;
                        if (v3 == null || (v3 = v3.getUser()) == null || (v3 = v3.getUid()) == null) {
                            v3 = "anonymous";
                        }
                        uid = v3;
                        this.this$0.getLocationService().startLocationTracking((String)uid);
                        confirmationMsg = "\ud83d\udccd Live location tracking started via voice command: \"" + ((VoiceCommand.TrackLocation)this.$command).getMatchedPhrase() + "\".";
                        GuardianViewModel.access$get_voiceCommandConfirmation$p(this.this$0).setValue((Object)confirmationMsg);
                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)uid);
                        this.L$1 = SpillingKt.nullOutSpilledVariable((Object)confirmationMsg);
                        this.label = 4;
                        v4 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast(confirmationMsg), (Continuation)this);
                        if (v4 == var6_2) {
                            return var6_2;
                        }
                        break block16;
                        {
                            case 4: {
                                confirmationMsg = (String)this.L$1;
                                uid = (String)this.L$0;
                                ResultKt.throwOnFailure((Object)$result);
                                v4 = $result;
                                break block16;
                            }
                        }
                    }
                    if (var2_3 instanceof VoiceCommand.Unknown) {
                        GuardianViewModel.access$get_voiceCommandConfirmation$p(this.this$0).setValue((Object)("Recognized: \"" + ((VoiceCommand.Unknown)this.$command).getSpokenText() + "\" (No actionable command matched)"));
                    } else {
                        throw new NoWhenBranchMatchedException();
                    }
                }
                return Unit.INSTANCE;
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void startVoiceRecognition(@NotNull Context context) {
        Intrinsics.checkNotNullParameter((Object)context, (String)"context");
        this.setVoiceSosEnabled(true);
    }

    public final void stopVoiceRecognition() {
        this.setVoiceSosEnabled(false);
    }

    public final void clearVoiceCommandConfirmation() {
        this._voiceCommandConfirmation.setValue(null);
    }

    public final void resolveAlert(@NotNull String alertId, @NotNull String notes) {
        Intrinsics.checkNotNullParameter((Object)alertId, (String)"alertId");
        Intrinsics.checkNotNullParameter((Object)notes, (String)"notes");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, alertId, notes, null){
            Object L$0;
            Object L$1;
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            final /* synthetic */ String $alertId;
            final /* synthetic */ String $notes;
            {
                this.this$0 = $receiver;
                this.$alertId = $alertId;
                this.$notes = $notes;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var5_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        var4_3 = this.this$0.getAuthState().getValue();
                        v0 = var4_3 instanceof AuthState.Success != false ? (AuthState.Success)var4_3 : null;
                        currentUser = v0 != null ? v0.getUser() : null;
                        v1 = currentUser;
                        if (v1 == null || (v1 = v1.getName()) == null) {
                            v1 = "Responder HQ";
                        }
                        resolverName = v1;
                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)currentUser);
                        this.L$1 = SpillingKt.nullOutSpilledVariable((Object)resolverName);
                        this.label = 1;
                        v2 = this.this$0.getDatabaseService().resolveSOS(this.$alertId, (String)resolverName, this.$notes, (Continuation<? super Unit>)((Continuation)this));
                        if (v2 == var5_2) {
                            return var5_2;
                        }
                        ** GOTO lbl24
                    }
                    case 1: {
                        resolverName = (String)this.L$1;
                        currentUser = (User)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v2 = $result;
lbl24:
                        // 2 sources

                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)currentUser);
                        this.L$1 = SpillingKt.nullOutSpilledVariable((Object)resolverName);
                        this.label = 2;
                        v3 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Alert successfully resolved."), (Continuation)this);
                        if (v3 == var5_2) {
                            return var5_2;
                        }
                        ** GOTO lbl36
                    }
                    case 2: {
                        resolverName = (String)this.L$1;
                        currentUser = (User)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v3 = $result;
lbl36:
                        // 2 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void bondDevice(@NotNull String name, @NotNull String mac, @NotNull String deviceId, @NotNull String firmware, int battery, int signal, @NotNull String health) {
        Intrinsics.checkNotNullParameter((Object)name, (String)"name");
        Intrinsics.checkNotNullParameter((Object)mac, (String)"mac");
        Intrinsics.checkNotNullParameter((Object)deviceId, (String)"deviceId");
        Intrinsics.checkNotNullParameter((Object)firmware, (String)"firmware");
        Intrinsics.checkNotNullParameter((Object)health, (String)"health");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, deviceId, name, battery, mac, firmware, signal, health, null){
            Object L$0;
            Object L$1;
            Object L$2;
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            final /* synthetic */ String $deviceId;
            final /* synthetic */ String $name;
            final /* synthetic */ int $battery;
            final /* synthetic */ String $mac;
            final /* synthetic */ String $firmware;
            final /* synthetic */ int $signal;
            final /* synthetic */ String $health;
            {
                this.this$0 = $receiver;
                this.$deviceId = $deviceId;
                this.$name = $name;
                this.$battery = $battery;
                this.$mac = $mac;
                this.$firmware = $firmware;
                this.$signal = $signal;
                this.$health = $health;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var5_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        var4_3 = this.this$0.getAuthState().getValue();
                        v0 = var4_3 instanceof AuthState.Success != false ? (AuthState.Success)var4_3 : null;
                        currentUser = v0 != null ? v0.getUser() : null;
                        v1 = currentUser;
                        if (v1 == null || (v1 = v1.getUid()) == null) {
                            v1 = "anonymous";
                        }
                        uid = v1;
                        newDevice = new Device(this.$deviceId, (String)uid, this.$name, "CONNECTED", this.$battery, this.$mac, System.currentTimeMillis(), this.$firmware, this.$signal, this.$health, false, 0, null, null, 0.0f, 0L, 0, 0, 0, null, 0.0, 0.0, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0xFFFFC00, null);
                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)currentUser);
                        this.L$1 = SpillingKt.nullOutSpilledVariable((Object)uid);
                        this.L$2 = SpillingKt.nullOutSpilledVariable((Object)newDevice);
                        this.label = 1;
                        v2 = this.this$0.getDatabaseService().updateDevice(newDevice, (Continuation<? super Device>)((Continuation)this));
                        if (v2 == var5_2) {
                            return var5_2;
                        }
                        ** GOTO lbl27
                    }
                    case 1: {
                        newDevice = (Device)this.L$2;
                        uid = (String)this.L$1;
                        currentUser = (User)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v2 = $result;
lbl27:
                        // 2 sources

                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)currentUser);
                        this.L$1 = SpillingKt.nullOutSpilledVariable((Object)uid);
                        this.L$2 = SpillingKt.nullOutSpilledVariable((Object)newDevice);
                        this.label = 2;
                        v3 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("ESP32 Wearable bound successfully!"), (Continuation)this);
                        if (v3 == var5_2) {
                            return var5_2;
                        }
                        ** GOTO lbl41
                    }
                    case 2: {
                        newDevice = (Device)this.L$2;
                        uid = (String)this.L$1;
                        currentUser = (User)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v3 = $result;
lbl41:
                        // 2 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public static /* synthetic */ void bondDevice$default(GuardianViewModel guardianViewModel, String string, String string2, String object, String string3, int n, int n2, String string4, int n3, Object object2) {
        if ((n3 & 4) != 0) {
            String string5 = UUID.randomUUID().toString();
            Intrinsics.checkNotNullExpressionValue((Object)string5, (String)"toString(...)");
            object = "esp32-" + StringsKt.take((String)string5, (int)8);
        }
        if ((n3 & 8) != 0) {
            string3 = "v1.2.4-esp32";
        }
        if ((n3 & 0x10) != 0) {
            n = 100;
        }
        if ((n3 & 0x20) != 0) {
            n2 = -67;
        }
        if ((n3 & 0x40) != 0) {
            string4 = "EXCELLENT";
        }
        guardianViewModel.bondDevice(string, string2, (String)object, string3, n, n2, string4);
    }

    public final void renameDevice(@NotNull String deviceId, @NotNull String newName) {
        Intrinsics.checkNotNullParameter((Object)deviceId, (String)"deviceId");
        Intrinsics.checkNotNullParameter((Object)newName, (String)"newName");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, deviceId, newName, null){
            Object L$0;
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            final /* synthetic */ String $deviceId;
            final /* synthetic */ String $newName;
            {
                this.this$0 = $receiver;
                this.$deviceId = $deviceId;
                this.$newName = $newName;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var3_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        v0 = this.this$0.getDatabaseService().renameDevice(this.$deviceId, this.$newName, (Continuation<? super Unit>)((Continuation)this));
                        ** if (v0 != var3_2) goto lbl11
lbl10:
                        // 1 sources

                        return var3_2;
lbl11:
                        // 1 sources

                        ** GOTO lbl17
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl17:
                        // 2 sources

                        this.label = 2;
                        v1 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Device renamed successfully!"), (Continuation)this);
                        ** if (v1 != var3_2) goto lbl21
lbl20:
                        // 1 sources

                        return var3_2;
lbl21:
                        // 1 sources

                        ** GOTO lbl39
                    }
                    case 2: {
                        try {
                            ResultKt.throwOnFailure((Object)$result);
                            v1 = $result;
                            ** GOTO lbl39
                        }
                        catch (Exception e) {
                            this.L$0 = SpillingKt.nullOutSpilledVariable((Object)e);
                            this.label = 3;
                            v2 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Failed to rename device: " + e.getLocalizedMessage()), (Continuation)this);
                            if (v2 == var3_2) {
                                return var3_2;
                            }
                            ** GOTO lbl39
                        }
                    }
                    case 3: {
                        e = (Exception)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v2 = $result;
lbl39:
                        // 4 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void unbondDevice(@NotNull String deviceId) {
        Intrinsics.checkNotNullParameter((Object)deviceId, (String)"deviceId");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, deviceId, null){
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            final /* synthetic */ String $deviceId;
            {
                this.this$0 = $receiver;
                this.$deviceId = $deviceId;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var2_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        v0 = this.this$0.getDatabaseService().deleteDevice(this.$deviceId, (Continuation<? super Unit>)((Continuation)this));
                        if (v0 == var2_2) {
                            return var2_2;
                        }
                        ** GOTO lbl13
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl13:
                        // 2 sources

                        this.label = 2;
                        v1 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Wearable device disconnected."), (Continuation)this);
                        if (v1 == var2_2) {
                            return var2_2;
                        }
                        ** GOTO lbl21
                    }
                    case 2: {
                        ResultKt.throwOnFailure((Object)$result);
                        v1 = $result;
lbl21:
                        // 2 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void saveEmergencyContact(@NotNull EmergencyContact contact) {
        Intrinsics.checkNotNullParameter((Object)contact, (String)"contact");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, contact, null){
            Object L$0;
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            final /* synthetic */ EmergencyContact $contact;
            {
                this.this$0 = $receiver;
                this.$contact = $contact;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var3_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        v0 = this.this$0.getDatabaseService().saveContact(this.$contact, (Continuation<? super EmergencyContact>)((Continuation)this));
                        ** if (v0 != var3_2) goto lbl11
lbl10:
                        // 1 sources

                        return var3_2;
lbl11:
                        // 1 sources

                        ** GOTO lbl17
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl17:
                        // 2 sources

                        this.label = 2;
                        v1 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Emergency contact saved successfully!"), (Continuation)this);
                        ** if (v1 != var3_2) goto lbl21
lbl20:
                        // 1 sources

                        return var3_2;
lbl21:
                        // 1 sources

                        ** GOTO lbl43
                    }
                    case 2: {
                        try {
                            ResultKt.throwOnFailure((Object)$result);
                            v1 = $result;
                            ** GOTO lbl43
                        }
                        catch (Exception e) {
                            v2 = GuardianViewModel.access$get_uiEvents$p(this.this$0);
                            v3 = e.getLocalizedMessage();
                            if (v3 == null) {
                                v3 = "Failed to save contact";
                            }
                            this.L$0 = SpillingKt.nullOutSpilledVariable((Object)e);
                            this.label = 3;
                            v4 = v2.emit((Object)new UiEvent.ShowToast(v3), (Continuation)this);
                            if (v4 == var3_2) {
                                return var3_2;
                            }
                            ** GOTO lbl43
                        }
                    }
                    case 3: {
                        e = (Exception)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v4 = $result;
lbl43:
                        // 4 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void deleteEmergencyContact(@NotNull String contactId) {
        Intrinsics.checkNotNullParameter((Object)contactId, (String)"contactId");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, contactId, null){
            Object L$0;
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            final /* synthetic */ String $contactId;
            {
                this.this$0 = $receiver;
                this.$contactId = $contactId;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var3_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        v0 = this.this$0.getDatabaseService().deleteContact(this.$contactId, (Continuation<? super Unit>)((Continuation)this));
                        ** if (v0 != var3_2) goto lbl11
lbl10:
                        // 1 sources

                        return var3_2;
lbl11:
                        // 1 sources

                        ** GOTO lbl17
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl17:
                        // 2 sources

                        this.label = 2;
                        v1 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Emergency contact deleted."), (Continuation)this);
                        ** if (v1 != var3_2) goto lbl21
lbl20:
                        // 1 sources

                        return var3_2;
lbl21:
                        // 1 sources

                        ** GOTO lbl43
                    }
                    case 2: {
                        try {
                            ResultKt.throwOnFailure((Object)$result);
                            v1 = $result;
                            ** GOTO lbl43
                        }
                        catch (Exception e) {
                            v2 = GuardianViewModel.access$get_uiEvents$p(this.this$0);
                            v3 = e.getLocalizedMessage();
                            if (v3 == null) {
                                v3 = "Failed to delete contact";
                            }
                            this.L$0 = SpillingKt.nullOutSpilledVariable((Object)e);
                            this.label = 3;
                            v4 = v2.emit((Object)new UiEvent.ShowToast(v3), (Continuation)this);
                            if (v4 == var3_2) {
                                return var3_2;
                            }
                            ** GOTO lbl43
                        }
                    }
                    case 3: {
                        e = (Exception)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v4 = $result;
lbl43:
                        // 4 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void startLocationTracking() {
        Object object = this.authState.getValue();
        Object object2 = object instanceof AuthState.Success ? (AuthState.Success)object : null;
        if (object2 == null || (object2 = ((AuthState.Success)object2).getUser()) == null || (object2 = ((User)object2).getUid()) == null) {
            object2 = "anonymous";
        }
        Object uid = object2;
        this.locationService.startLocationTracking((String)uid);
    }

    public final void stopLocationTracking() {
        this.locationService.stopLocationTracking();
    }

    public final void saveFavoritePlace(@NotNull String name, double lat, double lng, @NotNull String type) {
        Intrinsics.checkNotNullParameter((Object)name, (String)"name");
        Intrinsics.checkNotNullParameter((Object)type, (String)"type");
        this.locationService.saveFavoritePlace(name, lat, lng, type);
    }

    public final void deleteFavoritePlace(@NotNull String id) {
        Intrinsics.checkNotNullParameter((Object)id, (String)"id");
        this.locationService.deleteFavoritePlace(id);
    }

    @Nullable
    public final Pair<Double, Double> searchCoordinates(@NotNull String query) {
        Intrinsics.checkNotNullParameter((Object)query, (String)"query");
        return this.locationService.searchCoordinatesForQuery(query);
    }

    @Nullable
    public final Object getCurrentLocationOnce(@NotNull Continuation<? super Location> $completion) {
        return LocationService.getCurrentLocationOnce$default(this.locationService, 0L, $completion, 1, null);
    }

    public final void addTrustedPlace(@NotNull TrustedPlace place) {
        Intrinsics.checkNotNullParameter((Object)place, (String)"place");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, place, null){
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            final /* synthetic */ TrustedPlace $place;
            {
                this.this$0 = $receiver;
                this.$place = $place;
                super(2, $completion);
            }

            /*
             * Enabled force condition propagation
             * Lifted jumps to return sites
             */
            public final Object invokeSuspend(Object $result) {
                Object object = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        Object object2 = this.this$0.getTrustedPlacesService().addTrustedPlace(this.$place, (Continuation<? super Unit>)((Continuation)this));
                        if (object2 != object) return Unit.INSTANCE;
                        return object;
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        Object object2 = $result;
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void updateTrustedPlace(@NotNull TrustedPlace place) {
        Intrinsics.checkNotNullParameter((Object)place, (String)"place");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, place, null){
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            final /* synthetic */ TrustedPlace $place;
            {
                this.this$0 = $receiver;
                this.$place = $place;
                super(2, $completion);
            }

            /*
             * Enabled force condition propagation
             * Lifted jumps to return sites
             */
            public final Object invokeSuspend(Object $result) {
                Object object = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        Object object2 = this.this$0.getTrustedPlacesService().updateTrustedPlace(this.$place, (Continuation<? super Unit>)((Continuation)this));
                        if (object2 != object) return Unit.INSTANCE;
                        return object;
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        Object object2 = $result;
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void deleteTrustedPlace(@NotNull String placeId) {
        Intrinsics.checkNotNullParameter((Object)placeId, (String)"placeId");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, placeId, null){
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            final /* synthetic */ String $placeId;
            {
                this.this$0 = $receiver;
                this.$placeId = $placeId;
                super(2, $completion);
            }

            /*
             * Enabled force condition propagation
             * Lifted jumps to return sites
             */
            public final Object invokeSuspend(Object $result) {
                Object object = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        Object object2 = this.this$0.getTrustedPlacesService().deleteTrustedPlace(this.$placeId, (Continuation<? super Unit>)((Continuation)this));
                        if (object2 != object) return Unit.INSTANCE;
                        return object;
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        Object object2 = $result;
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void updateMapOptions(@NotNull String mode, boolean trafficEnabled) {
        Intrinsics.checkNotNullParameter((Object)mode, (String)"mode");
        this.locationService.updateMapOptions(mode, trafficEnabled);
    }

    public final void resetDistance() {
        this.locationService.resetDistance();
    }

    public final void searchLocation(@NotNull String query) {
        Job job;
        Intrinsics.checkNotNullParameter((Object)query, (String)"query");
        Pair<Double, Double> result = this.locationService.searchCoordinatesForQuery(query);
        if (result != null) {
            this.locationService.updateCurrentLocationManually(((Number)result.getFirst()).doubleValue(), ((Number)result.getSecond()).doubleValue());
            job = BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, query, null){
                int label;
                final /* synthetic */ GuardianViewModel this$0;
                final /* synthetic */ String $query;
                {
                    this.this$0 = $receiver;
                    this.$query = $query;
                    super(2, $completion);
                }

                /*
                 * Enabled force condition propagation
                 * Lifted jumps to return sites
                 */
                public final Object invokeSuspend(Object $result) {
                    Object object = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                    switch (this.label) {
                        case 0: {
                            ResultKt.throwOnFailure((Object)$result);
                            this.label = 1;
                            Object object2 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Moved map focus to: " + this.$query), (Continuation)this);
                            if (object2 != object) return Unit.INSTANCE;
                            return object;
                        }
                        case 1: {
                            ResultKt.throwOnFailure((Object)$result);
                            Object object2 = $result;
                            return Unit.INSTANCE;
                        }
                    }
                    throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                }

                public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                    return (Continuation)new /* invalid duplicate definition of identical inner class */;
                }

                public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                    return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
                }
            }), (int)3, null);
        } else {
            job = BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, query, null){
                int label;
                final /* synthetic */ GuardianViewModel this$0;
                final /* synthetic */ String $query;
                {
                    this.this$0 = $receiver;
                    this.$query = $query;
                    super(2, $completion);
                }

                /*
                 * Enabled force condition propagation
                 * Lifted jumps to return sites
                 */
                public final Object invokeSuspend(Object $result) {
                    Object object = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                    switch (this.label) {
                        case 0: {
                            ResultKt.throwOnFailure((Object)$result);
                            this.label = 1;
                            Object object2 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("No locations found for: '" + this.$query + "'"), (Continuation)this);
                            if (object2 != object) return Unit.INSTANCE;
                            return object;
                        }
                        case 1: {
                            ResultKt.throwOnFailure((Object)$result);
                            Object object2 = $result;
                            return Unit.INSTANCE;
                        }
                    }
                    throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                }

                public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                    return (Continuation)new /* invalid duplicate definition of identical inner class */;
                }

                public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                    return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
                }
            }), (int)3, null);
        }
    }

    public final void triggerEsp32SOS(@NotNull String triggerType) {
        Intrinsics.checkNotNullParameter((Object)triggerType, (String)"triggerType");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, triggerType, null){
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            final /* synthetic */ String $triggerType;
            {
                this.this$0 = $receiver;
                this.$triggerType = $triggerType;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var3_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        v0 = GuardianViewModel.initiateEmergencySequence$default(this.this$0, this.$triggerType, "ESP32-SOS-BAND-81F4", null, null, null, null, null, null, (Continuation)this, 252, null);
                        if (v0 == var3_2) {
                            return var3_2;
                        }
                        ** GOTO lbl13
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl13:
                        // 2 sources

                        model = (EmergencyModel)v0;
                        this.this$0.getAiAnalysisService().generateAnalysisForAlert(model.getEmergencyId(), this.$triggerType);
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public static /* synthetic */ void triggerEsp32SOS$default(GuardianViewModel guardianViewModel, String string, int n, Object object) {
        if ((n & 1) != 0) {
            string = "ESP32_BUTTON";
        }
        guardianViewModel.triggerEsp32SOS(string);
    }

    public final void acknowledgeEmergency() {
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
            Object L$0;
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            {
                this.this$0 = $receiver;
                super(2, $completion);
            }

            /*
             * Enabled force condition propagation
             * Lifted jumps to return sites
             */
            public final Object invokeSuspend(Object $result) {
                Object object = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        EmergencySession current = (EmergencySession)GuardianViewModel.access$get_emergencySession$p(this.this$0).getValue();
                        if (current.getActiveAlert() == null) return Unit.INSTANCE;
                        GuardianViewModel.access$get_emergencySession$p(this.this$0).setValue((Object)EmergencySession.copy$default(current, null, null, 0, null, null, null, 0, 0L, "RESPONDER ACKNOWLEDGED - DISPATCH CONFIRMED", false, true, false, 2815, null));
                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)current);
                        this.label = 1;
                        Object object2 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Emergency Acknowledged. Dispatching aid..."), (Continuation)this);
                        if (object2 != object) return Unit.INSTANCE;
                        return object;
                    }
                    case 1: {
                        EmergencySession current = (EmergencySession)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        Object object2 = $result;
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void updateResponderStatus(@NotNull String newStatus) {
        Intrinsics.checkNotNullParameter((Object)newStatus, (String)"newStatus");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, newStatus, null){
            Object L$0;
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            final /* synthetic */ String $newStatus;
            {
                this.this$0 = $receiver;
                this.$newStatus = $newStatus;
                super(2, $completion);
            }

            /*
             * Enabled force condition propagation
             * Lifted jumps to return sites
             */
            public final Object invokeSuspend(Object $result) {
                Object object = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        EmergencySession current = (EmergencySession)GuardianViewModel.access$get_emergencySession$p(this.this$0).getValue();
                        if (current.getActiveAlert() == null) return Unit.INSTANCE;
                        GuardianViewModel.access$get_emergencySession$p(this.this$0).setValue((Object)EmergencySession.copy$default(current, null, null, 0, null, null, null, 0, 0L, this.$newStatus, false, false, false, 3839, null));
                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)current);
                        this.label = 1;
                        Object object2 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Responder Status Updated: " + this.$newStatus), (Continuation)this);
                        if (object2 != object) return Unit.INSTANCE;
                        return object;
                    }
                    case 1: {
                        EmergencySession current = (EmergencySession)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        Object object2 = $result;
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void muteEmergencyAlarm() {
        this.alarmVibratorService.mute();
        this._isSirenPlaying.setValue((Object)false);
        this._emergencySession.setValue((Object)EmergencySession.copy$default((EmergencySession)this._emergencySession.getValue(), null, null, 0, null, null, null, 0, 0L, null, true, false, false, 3583, null));
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            {
                this.this$0 = $receiver;
                super(2, $completion);
            }

            /*
             * Enabled force condition propagation
             * Lifted jumps to return sites
             */
            public final Object invokeSuspend(Object $result) {
                Object object = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        Object object2 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Alarm audio muted."), (Continuation)this);
                        if (object2 != object) return Unit.INSTANCE;
                        return object;
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        Object object2 = $result;
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void markEmergencySafe() {
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
            Object L$0;
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            {
                this.this$0 = $receiver;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var3_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        current = (EmergencySession)GuardianViewModel.access$get_emergencySession$p(this.this$0).getValue();
                        if (current.getActiveAlert() == null) ** GOTO lbl43
                        this.this$0.getAlarmVibratorService().stopAlarm();
                        this.this$0.getAlarmVibratorService().stopVibration();
                        GuardianViewModel.access$get_isSirenPlaying$p(this.this$0).setValue((Object)Boxing.boxBoolean((boolean)false));
                        this.this$0.getEmergencyProvider().markEmergencySafe();
                        GuardianViewModel.access$get_emergencySession$p(this.this$0).setValue((Object)EmergencySession.copy$default(current, null, null, 0, null, null, null, 0, 0L, "MARKED SAFE - ALL CLEAR", false, false, true, 1791, null));
                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)current);
                        this.label = 1;
                        v0 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("User marked safe. Session auto-closing..."), (Continuation)this);
                        if (v0 == var3_2) {
                            return var3_2;
                        }
                        ** GOTO lbl22
                    }
                    case 1: {
                        current = (EmergencySession)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl22:
                        // 2 sources

                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)current);
                        this.label = 2;
                        v1 = DelayKt.delay((long)3000L, (Continuation)((Continuation)this));
                        if (v1 == var3_2) {
                            return var3_2;
                        }
                        ** GOTO lbl32
                    }
                    case 2: {
                        current = (EmergencySession)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v1 = $result;
lbl32:
                        // 2 sources

                        GuardianViewModel.access$get_emergencySession$p(this.this$0).setValue((Object)new EmergencySession(null, null, 0, null, null, null, 0, 0L, null, false, false, false, 4095, null));
                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)current);
                        this.label = 3;
                        v2 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)UiEvent.NavigateToHome.INSTANCE, (Continuation)this);
                        if (v2 == var3_2) {
                            return var3_2;
                        }
                        ** GOTO lbl43
                    }
                    case 3: {
                        current = (EmergencySession)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v2 = $result;
lbl43:
                        // 3 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void cancelEmergencyWithPin(@NotNull String pin, @NotNull Function1<? super Boolean, Unit> callback) {
        Intrinsics.checkNotNullParameter((Object)pin, (String)"pin");
        Intrinsics.checkNotNullParameter(callback, (String)"callback");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, pin, callback, null){
            Object L$0;
            boolean Z$0;
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            final /* synthetic */ String $pin;
            final /* synthetic */ Function1<Boolean, Unit> $callback;
            {
                this.this$0 = $receiver;
                this.$pin = $pin;
                this.$callback = $callback;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var4_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        expectedPin = this.this$0.getSecurityService().getEmergencyPin();
                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)expectedPin);
                        this.label = 1;
                        v0 = this.this$0.getEmergencyProvider().cancelEmergency(this.$pin, expectedPin, "Cancelled securely with PIN verification.", (Continuation<? super Boolean>)((Continuation)this));
                        if (v0 == var4_2) {
                            return var4_2;
                        }
                        ** GOTO lbl16
                    }
                    case 1: {
                        expectedPin = (String)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl16:
                        // 2 sources

                        if (!(success = ((Boolean)v0).booleanValue())) break;
                        Log.d((String)"SOS_ESP32", (String)"SOS CANCELLED");
                        this.this$0.getDeviceService().resetEsp32();
                        this.this$0.getAlarmVibratorService().stopAlarm();
                        this.this$0.getAlarmVibratorService().stopVibration();
                        this.this$0.getAlarmVibratorService().cleanUp();
                        GuardianViewModel.access$get_emergencySession$p(this.this$0).setValue((Object)new EmergencySession(null, null, 0, null, null, null, 0, 0L, null, false, false, false, 4095, null));
                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)expectedPin);
                        this.Z$0 = success;
                        this.label = 2;
                        v1 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("SOS Session Cancelled successfully with PIN."), (Continuation)this);
                        if (v1 == var4_2) {
                            return var4_2;
                        }
                        ** GOTO lbl36
                    }
                    case 2: {
                        success = this.Z$0;
                        expectedPin = (String)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v1 = $result;
lbl36:
                        // 2 sources

                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)expectedPin);
                        this.Z$0 = success;
                        this.label = 3;
                        v2 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)UiEvent.NavigateToHome.INSTANCE, (Continuation)this);
                        if (v2 == var4_2) {
                            return var4_2;
                        }
                        ** GOTO lbl61
                    }
                    case 3: {
                        success = this.Z$0;
                        expectedPin = (String)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v2 = $result;
                        ** GOTO lbl61
                    }
                }
                this.L$0 = SpillingKt.nullOutSpilledVariable((Object)expectedPin);
                this.Z$0 = success;
                this.label = 4;
                v3 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Incorrect Emergency Security PIN."), (Continuation)this);
                if (v3 == var4_2) {
                    return var4_2;
                }
                ** GOTO lbl61
                {
                    case 4: {
                        success = this.Z$0;
                        expectedPin = (String)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v3 = $result;
lbl61:
                        // 4 sources

                        this.$callback.invoke((Object)Boxing.boxBoolean((boolean)(success != false)));
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void endEmergencySOS(@NotNull String notes) {
        Intrinsics.checkNotNullParameter((Object)notes, (String)"notes");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, notes, null){
            Object L$0;
            Object L$1;
            Object L$2;
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            final /* synthetic */ String $notes;
            {
                this.this$0 = $receiver;
                this.$notes = $notes;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var22_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        current = (EmergencySession)GuardianViewModel.access$get_emergencySession$p(this.this$0).getValue();
                        v0 = current.getActiveAlert();
                        v1 = alertId = v0 != null ? v0.getId() : null;
                        if (alertId == null) ** GOTO lbl63
                        var6_7 = this.this$0.getAuthState().getValue();
                        v2 = var6_7 instanceof AuthState.Success != false ? (AuthState.Success)var6_7 : null;
                        if (v2 == null || (v2 = v2.getUser()) == null || (v2 = v2.getName()) == null) {
                            v2 = "Operator";
                        }
                        userName = v2;
                        this.L$0 = current;
                        this.L$1 = alertId;
                        this.L$2 = userName;
                        this.label = 1;
                        v3 = this.this$0.getDatabaseService().resolveSOS(alertId, (String)userName, this.$notes, (Continuation<? super Unit>)((Continuation)this));
                        if (v3 == var22_2) {
                            return var22_2;
                        }
                        ** GOTO lbl28
                    }
                    case 1: {
                        userName = (String)this.L$2;
                        alertId = (String)this.L$1;
                        current = (EmergencySession)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v3 = $result;
lbl28:
                        // 2 sources

                        this.this$0.getEmergencyProvider().markEmergencySafe();
                        duration = (System.currentTimeMillis() - current.getStartTimeMs()) / (long)1000;
                        $this$map\1 = (Iterable)this.this$0.getContacts().getValue();
                        $i$f$map\1\985 = false;
                        var10_13 = $this$map\1;
                        destination\2 = new ArrayList<E>(CollectionsKt.collectionSizeOrDefault((Iterable)$this$map\1, (int)10));
                        $i$f$mapTo\2\1295 = false;
                        for (T item\2 : $this$mapTo\2) {
                            var15_20 = (EmergencyContact)item\2;
                            var21_23 = destination\2;
                            $i$a$-map-GuardianViewModel$endEmergencySOS$1$activeContacts$1\3\1297\0 = false;
                            var21_23.add(it\3.getName());
                        }
                        activeContacts = (List)destination\2;
                        v4 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                        Intrinsics.checkNotNullExpressionValue((Object)v4, (String)"format(...)");
                        var9_12 = v4;
                        v5 = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
                        Intrinsics.checkNotNullExpressionValue((Object)v5, (String)"format(...)");
                        var10_13 = v5;
                        var11_15 = duration > 0L ? duration : 45L;
                        var13_18 = current.getActiveAlert().getLatitude();
                        var15_21 = current.getActiveAlert().getLongitude();
                        var17_25 = Intrinsics.areEqual((Object)current.getActiveAlert().getTriggerType(), (Object)"FALL_DETECTED") != false ? "CRITICAL" : "HIGH";
                        if (((Collection)activeContacts).isEmpty() == false) {
                            v6 = activeContacts;
                        } else {
                            var19_26 = new String[]{"Dr. Jenkins", "Warden Vance"};
                            v6 = CollectionsKt.listOf((Object[])var19_26);
                        }
                        var18_28 = v6;
                        var19_27 = Intrinsics.areEqual((Object)current.getActiveAlert().getTriggerType(), (Object)"FALL_DETECTED") != false ? 94 : 100;
                        var20_29 = current.getActiveAlert().getTriggerType();
                        historyItem = new HistoryModel(alertId, var9_12, (String)var10_13, var11_15, 14L, "GPS Coordinate Plot", var13_18, var15_21, var19_27, var17_25, var18_28, null, var20_29, this.$notes, (String)userName, 2048, null);
                        this.this$0.getHistoryProvider().addHistoryRecord(historyItem);
lbl63:
                        // 2 sources

                        this.this$0.getAlarmVibratorService().cleanUp();
                        GuardianViewModel.access$get_emergencySession$p(this.this$0).setValue((Object)new EmergencySession(null, null, 0, null, null, null, 0, 0L, null, false, false, false, 4095, null));
                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)current);
                        this.L$1 = SpillingKt.nullOutSpilledVariable((Object)alertId);
                        this.L$2 = null;
                        this.label = 2;
                        v7 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Emergency resolved. Returning to Home."), (Continuation)this);
                        if (v7 == var22_2) {
                            return var22_2;
                        }
                        ** GOTO lbl78
                    }
                    case 2: {
                        alertId = (String)this.L$1;
                        current = (EmergencySession)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v7 = $result;
lbl78:
                        // 2 sources

                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)current);
                        this.L$1 = SpillingKt.nullOutSpilledVariable((Object)alertId);
                        this.label = 3;
                        v8 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)UiEvent.NavigateToHome.INSTANCE, (Continuation)this);
                        if (v8 == var22_2) {
                            return var22_2;
                        }
                        ** GOTO lbl90
                    }
                    case 3: {
                        alertId = (String)this.L$1;
                        current = (EmergencySession)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v8 = $result;
lbl90:
                        // 2 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public static /* synthetic */ void endEmergencySOS$default(GuardianViewModel guardianViewModel, String string, int n, Object object) {
        if ((n & 1) != 0) {
            string = "Resolved by responder from app dashboard.";
        }
        guardianViewModel.endEmergencySOS(string);
    }

    public final void deleteHistoryItem(@NotNull String id) {
        Intrinsics.checkNotNullParameter((Object)id, (String)"id");
        this.historyProvider.deleteHistoryRecord(id);
    }

    @NotNull
    public final String getHistoryCSVString() {
        return this.historyProvider.exportToCSV();
    }

    @NotNull
    public final String getHistoryPDFReportText() {
        return this.historyProvider.exportToPDF();
    }

    public final void markNotificationAsRead(@NotNull String id) {
        Intrinsics.checkNotNullParameter((Object)id, (String)"id");
        this.notificationService.markAsRead(id);
    }

    public final void markNotificationNewAsRead(@NotNull String id) {
        Intrinsics.checkNotNullParameter((Object)id, (String)"id");
        this.notificationProvider.markAsRead(id);
    }

    public final void markAllNotificationsAsRead() {
        this.notificationService.markAllAsRead();
    }

    public final void markAllNotificationsNewAsRead() {
        this.notificationProvider.markAllAsRead();
    }

    public final void deleteNotification(@NotNull String id) {
        Intrinsics.checkNotNullParameter((Object)id, (String)"id");
        this.notificationService.deleteNotification(id);
    }

    public final void deleteNotificationNew(@NotNull String id) {
        Intrinsics.checkNotNullParameter((Object)id, (String)"id");
        this.notificationProvider.deleteNotification(id);
    }

    public final void refreshDeviceStatus() {
        this.deviceService.refreshDeviceStatus();
    }

    public final void restartDevice(@NotNull String deviceId) {
        Intrinsics.checkNotNullParameter((Object)deviceId, (String)"deviceId");
        this.deviceService.restartDevice(deviceId);
    }

    public final void runDiagnostics(@NotNull String deviceId) {
        Intrinsics.checkNotNullParameter((Object)deviceId, (String)"deviceId");
        this.deviceService.runDiagnostics(deviceId);
    }

    public final void cleanDiagnosticsLog() {
        this.deviceService.cleanDiagnosticsLog();
    }

    public final void setNetworkAvailable(boolean available) {
        this.deviceService.setNetworkAvailable(available);
    }

    public final void addCommLog(@NotNull String log) {
        Intrinsics.checkNotNullParameter((Object)log, (String)"log");
        this.deviceService.addCommLog(log);
    }

    public final void clearCommLogs() {
        this.deviceService.clearCommLogs();
    }

    public final void authenticateAndRegisterESP32(@NotNull String name, @NotNull String mac, @NotNull String token, @NotNull String firmware, @NotNull Function1<? super Result<Device>, Unit> onResult) {
        Intrinsics.checkNotNullParameter((Object)name, (String)"name");
        Intrinsics.checkNotNullParameter((Object)mac, (String)"mac");
        Intrinsics.checkNotNullParameter((Object)token, (String)"token");
        Intrinsics.checkNotNullParameter((Object)firmware, (String)"firmware");
        Intrinsics.checkNotNullParameter(onResult, (String)"onResult");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, name, mac, token, firmware, onResult, null){
            Object L$0;
            Object L$1;
            Object L$2;
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            final /* synthetic */ String $name;
            final /* synthetic */ String $mac;
            final /* synthetic */ String $token;
            final /* synthetic */ String $firmware;
            final /* synthetic */ Function1<Result<Device>, Unit> $onResult;
            {
                this.this$0 = $receiver;
                this.$name = $name;
                this.$mac = $mac;
                this.$token = $token;
                this.$firmware = $firmware;
                this.$onResult = $onResult;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var5_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        var4_3 = this.this$0.getAuthState().getValue();
                        v0 = var4_3 instanceof AuthState.Success != false ? (AuthState.Success)var4_3 : null;
                        currentUser = v0 != null ? v0.getUser() : null;
                        v1 = currentUser;
                        if (v1 == null || (v1 = v1.getUid()) == null) {
                            v1 = "anonymous";
                        }
                        uid = v1;
                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)currentUser);
                        this.L$1 = SpillingKt.nullOutSpilledVariable((Object)uid);
                        this.label = 1;
                        v2 = this.this$0.getDeviceService().authenticateAndRegisterESP32-hUnOzRk((String)uid, this.$name, this.$mac, this.$token, this.$firmware, (Continuation<? super Result<Device>>)((Continuation)this));
                        if (v2 == var5_2) {
                            return var5_2;
                        }
                        ** GOTO lbl24
                    }
                    case 1: {
                        uid = (String)this.L$1;
                        currentUser = (User)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v2 = ((Result)$result).unbox-impl();
lbl24:
                        // 2 sources

                        result = v2;
                        this.$onResult.invoke((Object)Result.box-impl((Object)result));
                        if (!Result.isSuccess-impl((Object)result)) break;
                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)currentUser);
                        this.L$1 = SpillingKt.nullOutSpilledVariable((Object)uid);
                        this.L$2 = SpillingKt.nullOutSpilledVariable((Object)result);
                        this.label = 2;
                        v3 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("ESP32 Handshake Authenticated & Registered!"), (Continuation)this);
                        if (v3 == var5_2) {
                            return var5_2;
                        }
                        ** GOTO lbl58
                    }
                    case 2: {
                        result = this.L$2;
                        uid = (String)this.L$1;
                        currentUser = (User)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v3 = $result;
                        ** GOTO lbl58
                    }
                }
                v4 = Result.exceptionOrNull-impl((Object)result);
                this.L$0 = SpillingKt.nullOutSpilledVariable((Object)currentUser);
                this.L$1 = SpillingKt.nullOutSpilledVariable((Object)uid);
                this.L$2 = SpillingKt.nullOutSpilledVariable((Object)result);
                this.label = 3;
                v5 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Registration Handshake Failed: " + (v4 != null ? v4.getMessage() : null)), (Continuation)this);
                if (v5 == var5_2) {
                    return var5_2;
                }
                ** GOTO lbl58
                {
                    case 3: {
                        result = this.L$2;
                        uid = (String)this.L$1;
                        currentUser = (User)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v5 = $result;
lbl58:
                        // 4 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void resetEsp32() {
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            {
                this.this$0 = $receiver;
                super(2, $completion);
            }

            public final Object invokeSuspend(Object $result) {
                IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.this$0.getDeviceService().resetEsp32();
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    @NotNull
    public final StateFlow<Boolean> isGpsDisabled() {
        return this.isGpsDisabled;
    }

    @NotNull
    public final StateFlow<Boolean> isWeakGps() {
        return this.isWeakGps;
    }

    public final void setGpsDisabled(boolean disabled) {
    }

    public final void setWeakGps(boolean weak) {
    }

    @NotNull
    public final MutableStateFlow<Boolean> isOfflineMode() {
        return this.isOfflineMode;
    }

    @NotNull
    public final MutableStateFlow<Boolean> isSlowNetwork() {
        return this.isSlowNetwork;
    }

    public final void setOfflineMode(boolean enabled) {
        this.databaseService.isOfflineMode().setValue((Object)enabled);
    }

    public final void setSlowNetwork(boolean enabled) {
        this.databaseService.isSlowNetwork().setValue((Object)enabled);
    }

    public final void uploadTestSOS() {
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
            Object L$0;
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            {
                this.this$0 = $receiver;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var3_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        v0 = this.this$0.getDatabaseService().uploadTestSOS((Continuation<? super Alert>)((Continuation)this));
                        ** if (v0 != var3_2) goto lbl11
lbl10:
                        // 1 sources

                        return var3_2;
lbl11:
                        // 1 sources

                        ** GOTO lbl17
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl17:
                        // 2 sources

                        this.label = 2;
                        v1 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Test SOS Uploaded"), (Continuation)this);
                        ** if (v1 != var3_2) goto lbl21
lbl20:
                        // 1 sources

                        return var3_2;
lbl21:
                        // 1 sources

                        ** GOTO lbl39
                    }
                    case 2: {
                        try {
                            ResultKt.throwOnFailure((Object)$result);
                            v1 = $result;
                            ** GOTO lbl39
                        }
                        catch (Exception e) {
                            this.L$0 = SpillingKt.nullOutSpilledVariable((Object)e);
                            this.label = 3;
                            v2 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Upload Failed: " + e.getMessage()), (Continuation)this);
                            if (v2 == var3_2) {
                                return var3_2;
                            }
                            ** GOTO lbl39
                        }
                    }
                    case 3: {
                        e = (Exception)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v2 = $result;
lbl39:
                        // 4 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void downloadTestData() {
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
            Object L$0;
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            {
                this.this$0 = $receiver;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var3_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        v0 = this.this$0.getDatabaseService().downloadTestData((Continuation<? super Unit>)((Continuation)this));
                        ** if (v0 != var3_2) goto lbl11
lbl10:
                        // 1 sources

                        return var3_2;
lbl11:
                        // 1 sources

                        ** GOTO lbl17
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl17:
                        // 2 sources

                        this.label = 2;
                        v1 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Test Data Downloaded"), (Continuation)this);
                        ** if (v1 != var3_2) goto lbl21
lbl20:
                        // 1 sources

                        return var3_2;
lbl21:
                        // 1 sources

                        ** GOTO lbl39
                    }
                    case 2: {
                        try {
                            ResultKt.throwOnFailure((Object)$result);
                            v1 = $result;
                            ** GOTO lbl39
                        }
                        catch (Exception e) {
                            this.L$0 = SpillingKt.nullOutSpilledVariable((Object)e);
                            this.label = 3;
                            v2 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Download Failed: " + e.getMessage()), (Continuation)this);
                            if (v2 == var3_2) {
                                return var3_2;
                            }
                            ** GOTO lbl39
                        }
                    }
                    case 3: {
                        e = (Exception)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v2 = $result;
lbl39:
                        // 4 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    @NotNull
    public final StateFlow<List<DeveloperLog>> getDeveloperLogs() {
        return this.developerLogs;
    }

    public final void addDeveloperLog(@NotNull String event, @NotNull String status) {
        Intrinsics.checkNotNullParameter((Object)event, (String)"event");
        Intrinsics.checkNotNullParameter((Object)status, (String)"status");
        this.databaseService.addDeveloperLog(event, status);
    }

    public final void clearDeveloperLogs() {
        this.databaseService.clearDeveloperLogs();
    }

    public final void deleteTestRecords() {
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
            Object L$0;
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            {
                this.this$0 = $receiver;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var3_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        v0 = this.this$0.getDatabaseService().deleteTestRecords((Continuation<? super Unit>)((Continuation)this));
                        ** if (v0 != var3_2) goto lbl11
lbl10:
                        // 1 sources

                        return var3_2;
lbl11:
                        // 1 sources

                        ** GOTO lbl17
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl17:
                        // 2 sources

                        this.label = 2;
                        v1 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Test Records Deleted"), (Continuation)this);
                        ** if (v1 != var3_2) goto lbl21
lbl20:
                        // 1 sources

                        return var3_2;
lbl21:
                        // 1 sources

                        ** GOTO lbl39
                    }
                    case 2: {
                        try {
                            ResultKt.throwOnFailure((Object)$result);
                            v1 = $result;
                            ** GOTO lbl39
                        }
                        catch (Exception e) {
                            this.L$0 = SpillingKt.nullOutSpilledVariable((Object)e);
                            this.label = 3;
                            v2 = GuardianViewModel.access$get_uiEvents$p(this.this$0).emit((Object)new UiEvent.ShowToast("Delete Failed: " + e.getMessage()), (Continuation)this);
                            if (v2 == var3_2) {
                                return var3_2;
                            }
                            ** GOTO lbl39
                        }
                    }
                    case 3: {
                        e = (Exception)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v2 = $result;
lbl39:
                        // 4 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void setCustomLocation(double lat, double lng) {
        LocationService.setCustomLocation$default(this.locationService, lat, lng, 0.0f, 4, null);
    }

    public final void disconnectDevice(@NotNull String deviceId) {
        Intrinsics.checkNotNullParameter((Object)deviceId, (String)"deviceId");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, deviceId, null){
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            final /* synthetic */ String $deviceId;
            {
                this.this$0 = $receiver;
                this.$deviceId = $deviceId;
                super(2, $completion);
            }

            /*
             * Enabled force condition propagation
             * Lifted jumps to return sites
             */
            public final Object invokeSuspend(Object $result) {
                Object object = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        Object object2 = this.this$0.getDeviceService().handleDeviceDisconnect(this.$deviceId, (Continuation<? super Unit>)((Continuation)this));
                        if (object2 != object) return Unit.INSTANCE;
                        return object;
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        Object object2 = $result;
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void connectDevice(@NotNull String deviceId) {
        Intrinsics.checkNotNullParameter((Object)deviceId, (String)"deviceId");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, deviceId, null){
            Object L$0;
            int label;
            final /* synthetic */ GuardianViewModel this$0;
            final /* synthetic */ String $deviceId;
            {
                this.this$0 = $receiver;
                this.$deviceId = $deviceId;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var10_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        var3_3 = (Iterable)this.this$0.getDatabaseService().getDevices().getValue();
                        var4_5 = this.$deviceId;
                        var5_6 = var3_3;
                        for (T var7_8 : var5_6) {
                            it\2 = (Device)var7_8;
                            $i$a$-find-GuardianViewModel$connectDevice$1$device$1\2\1194\0 = false;
                            if (!Intrinsics.areEqual((Object)it\2.getDeviceId(), (Object)var4_5)) continue;
                            v0 = var7_8;
                            ** GOTO lbl15
                        }
                        v0 = null;
lbl15:
                        // 2 sources

                        device = v0;
                        if (device == null) ** GOTO lbl29
                        var3_4 = System.currentTimeMillis();
                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)device);
                        this.label = 1;
                        v1 = this.this$0.getDatabaseService().updateDevice(Device.copy$default(device, null, null, null, "CONNECTED", 0, null, var3_4, null, 0, null, false, 0, null, null, 0.0f, 0L, 0, 0, 0, "ONLINE", 0.0, 0.0, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0xFF7FFB7, null), (Continuation<? super Device>)((Continuation)this));
                        if (v1 == var10_2) {
                            return var10_2;
                        }
                        ** GOTO lbl28
                    }
                    case 1: {
                        device = (Device)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v1 = $result;
lbl28:
                        // 2 sources

                        this.this$0.getDeviceService().addCommLog("\u2705 DEVICE_CONNECTED message processed.");
lbl29:
                        // 2 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    protected void onCleared() {
        super.onCleared();
        this.alarmVibratorService.cleanUp();
        this.fallDetectionService.cleanup();
        this.voiceSosService.cleanup();
        this.deviceService.stopEsp32Polling();
    }

    public final void startEsp32Polling() {
        this.deviceService.startEsp32Polling();
    }

    public final void stopEsp32Polling() {
        this.deviceService.stopEsp32Polling();
    }

    public final void triggerManualHeartbeatCheck(@NotNull String deviceId) {
        Intrinsics.checkNotNullParameter((Object)deviceId, (String)"deviceId");
        this.deviceService.triggerManualHeartbeatCheck(deviceId);
    }

    private static final Unit _init_$lambda$0(GuardianViewModel this$0) {
        this$0.triggerFallDetectedSOS();
        return Unit.INSTANCE;
    }

    private static final Unit _init_$lambda$1(GuardianViewModel this$0, Mpu6050Reading reading, String eventId) {
        Intrinsics.checkNotNullParameter((Object)reading, (String)"reading");
        Intrinsics.checkNotNullParameter((Object)eventId, (String)"eventId");
        Log.d((String)"GuardianViewModel", (String)("MOTION: possible fall detected from MPU6050 event " + eventId + " (MAG=" + reading.getAccelerationMagnitudeG() + "g)"));
        if (!this$0.emergencyService.isEmergencyActive() && !Intrinsics.areEqual((Object)this$0.fallDetectionService.getCurrentState().getValue(), (Object)"FALL_COUNTDOWN")) {
            this$0.fallDetectionService.triggerFall();
        }
        return Unit.INSTANCE;
    }

    private static final Unit _init_$lambda$2(GuardianViewModel this$0) {
        this$0.triggerTimerSOS();
        return Unit.INSTANCE;
    }

    private static final Unit _init_$lambda$3(GuardianViewModel this$0) {
        this$0.triggerFallDetectedSOS();
        return Unit.INSTANCE;
    }

    private static final Unit _init_$lambda$4(GuardianViewModel this$0, String matchedPhrase, int confidence) {
        Intrinsics.checkNotNullParameter((Object)matchedPhrase, (String)"matchedPhrase");
        this$0.triggerVoiceSOS(matchedPhrase, confidence);
        return Unit.INSTANCE;
    }

    private static final Unit _init_$lambda$5(GuardianViewModel this$0, VoiceCommand command, int confidence) {
        Intrinsics.checkNotNullParameter((Object)command, (String)"command");
        this$0.handleVoiceCommand(command, confidence);
        return Unit.INSTANCE;
    }

    public static final /* synthetic */ MutableSharedFlow access$get_uiEvents$p(GuardianViewModel $this) {
        return $this._uiEvents;
    }

    public static final /* synthetic */ MutableStateFlow access$get_isBackupRunning$p(GuardianViewModel $this) {
        return $this._isBackupRunning;
    }

    public static final /* synthetic */ MutableStateFlow access$get_lastBackupTime$p(GuardianViewModel $this) {
        return $this._lastBackupTime;
    }

    public static final /* synthetic */ Object access$initiateEmergencySequence(GuardianViewModel $this, String triggerSource, String deviceId, Double lat, Double lng, Float accuracy, Double altitude, Float speed, Float bearing, Continuation $completion) {
        return $this.initiateEmergencySequence(triggerSource, deviceId, lat, lng, accuracy, altitude, speed, bearing, (Continuation<? super EmergencyModel>)$completion);
    }

    public static final /* synthetic */ MutableStateFlow access$get_sosWorkflowState$p(GuardianViewModel $this) {
        return $this._sosWorkflowState;
    }

    public static final /* synthetic */ MutableStateFlow access$get_voiceCommandConfirmation$p(GuardianViewModel $this) {
        return $this._voiceCommandConfirmation;
    }

    public static final /* synthetic */ MutableStateFlow access$get_isSirenPlaying$p(GuardianViewModel $this) {
        return $this._isSirenPlaying;
    }

    @Metadata(mv={2, 2, 0}, k=1, xi=48, d1={"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b7\u0018\u00002\u00020\u0001:\u0004\u0004\u0005\u0006\u0007B\t\b\u0004\u00a2\u0006\u0004\b\u0002\u0010\u0003\u0082\u0001\u0004\b\t\n\u000b\u00a8\u0006\f"}, d2={"Lcom/example/ui/GuardianViewModel$UiEvent;", "", "<init>", "()V", "ShowToast", "NavigateToHome", "NavigateToLogin", "NavigateToEmergency", "Lcom/example/ui/GuardianViewModel$UiEvent$NavigateToEmergency;", "Lcom/example/ui/GuardianViewModel$UiEvent$NavigateToHome;", "Lcom/example/ui/GuardianViewModel$UiEvent$NavigateToLogin;", "Lcom/example/ui/GuardianViewModel$UiEvent$ShowToast;", "app"})
    @StabilityInferred(parameters=1)
    public static abstract class UiEvent {
        public static final int $stable;

        private UiEvent() {
        }

        public /* synthetic */ UiEvent(DefaultConstructorMarker $constructor_marker) {
            this();
        }

        @Metadata(mv={2, 2, 0}, k=1, xi=48, d1={"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c7\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003\u00a8\u0006\u0004"}, d2={"Lcom/example/ui/GuardianViewModel$UiEvent$NavigateToEmergency;", "Lcom/example/ui/GuardianViewModel$UiEvent;", "<init>", "()V", "app"})
        @StabilityInferred(parameters=1)
        public static final class NavigateToEmergency
        extends UiEvent {
            @NotNull
            public static final NavigateToEmergency INSTANCE = new NavigateToEmergency();
            public static final int $stable;

            private NavigateToEmergency() {
                super(null);
            }
        }

        @Metadata(mv={2, 2, 0}, k=1, xi=48, d1={"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c7\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003\u00a8\u0006\u0004"}, d2={"Lcom/example/ui/GuardianViewModel$UiEvent$NavigateToHome;", "Lcom/example/ui/GuardianViewModel$UiEvent;", "<init>", "()V", "app"})
        @StabilityInferred(parameters=1)
        public static final class NavigateToHome
        extends UiEvent {
            @NotNull
            public static final NavigateToHome INSTANCE = new NavigateToHome();
            public static final int $stable;

            private NavigateToHome() {
                super(null);
            }
        }

        @Metadata(mv={2, 2, 0}, k=1, xi=48, d1={"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c7\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003\u00a8\u0006\u0004"}, d2={"Lcom/example/ui/GuardianViewModel$UiEvent$NavigateToLogin;", "Lcom/example/ui/GuardianViewModel$UiEvent;", "<init>", "()V", "app"})
        @StabilityInferred(parameters=1)
        public static final class NavigateToLogin
        extends UiEvent {
            @NotNull
            public static final NavigateToLogin INSTANCE = new NavigateToLogin();
            public static final int $stable;

            private NavigateToLogin() {
                super(null);
            }
        }

        @Metadata(mv={2, 2, 0}, k=1, xi=48, d1={"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0087\b\u0018\u00002\u00020\u0001B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0004\b\u0004\u0010\u0005J\t\u0010\b\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\t\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\n\u001a\u00020\u000b2\b\u0010\f\u001a\u0004\u0018\u00010\rH\u00d6\u0003J\t\u0010\u000e\u001a\u00020\u000fH\u00d6\u0001J\t\u0010\u0010\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007\u00a8\u0006\u0011"}, d2={"Lcom/example/ui/GuardianViewModel$UiEvent$ShowToast;", "Lcom/example/ui/GuardianViewModel$UiEvent;", "message", "", "<init>", "(Ljava/lang/String;)V", "getMessage", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app"})
        @StabilityInferred(parameters=1)
        public static final class ShowToast
        extends UiEvent {
            @NotNull
            private final String message;
            public static final int $stable;

            public ShowToast(@NotNull String message) {
                Intrinsics.checkNotNullParameter((Object)message, (String)"message");
                super(null);
                this.message = message;
            }

            @NotNull
            public final String getMessage() {
                return this.message;
            }

            @NotNull
            public final String component1() {
                return this.message;
            }

            @NotNull
            public final ShowToast copy(@NotNull String message) {
                Intrinsics.checkNotNullParameter((Object)message, (String)"message");
                return new ShowToast(message);
            }

            public static /* synthetic */ ShowToast copy$default(ShowToast showToast, String string, int n, Object object) {
                if ((n & 1) != 0) {
                    string = showToast.message;
                }
                return showToast.copy(string);
            }

            @NotNull
            public String toString() {
                return "ShowToast(message=" + this.message + ")";
            }

            public int hashCode() {
                return this.message.hashCode();
            }

            public boolean equals(@Nullable Object other) {
                if (this == other) {
                    return true;
                }
                if (!(other instanceof ShowToast)) {
                    return false;
                }
                ShowToast showToast = (ShowToast)other;
                return Intrinsics.areEqual((Object)this.message, (Object)showToast.message);
            }
        }
    }
}
