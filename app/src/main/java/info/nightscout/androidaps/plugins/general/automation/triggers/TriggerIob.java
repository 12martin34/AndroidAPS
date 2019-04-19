package info.nightscout.androidaps.plugins.general.automation.triggers;

import android.widget.LinearLayout;

import com.google.common.base.Optional;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.IobTotal;
import info.nightscout.androidaps.data.Profile;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.configBuilder.ProfileFunctions;
import info.nightscout.androidaps.plugins.general.automation.elements.Comparator;
import info.nightscout.androidaps.plugins.general.automation.elements.InputInsulin;
import info.nightscout.androidaps.plugins.general.automation.elements.Label;
import info.nightscout.androidaps.plugins.general.automation.elements.LayoutBuilder;
import info.nightscout.androidaps.plugins.iob.iobCobCalculator.IobCobCalculatorPlugin;
import info.nightscout.androidaps.utils.DateUtil;
import info.nightscout.androidaps.utils.JsonHelper;
import info.nightscout.androidaps.utils.T;

public class TriggerIob extends Trigger {
    private static Logger log = LoggerFactory.getLogger(L.AUTOMATION);

    private InputInsulin insulin;
    private Comparator comparator = new Comparator();
    private long lastRun;

    public TriggerIob() {
        super();
    }

    TriggerIob(TriggerIob triggerIob) {
        super();
        comparator = triggerIob.comparator;
        lastRun = triggerIob.lastRun;
        insulin = triggerIob.insulin;
    }

    public double getThreshold() {
        return insulin.getValue();
    }

    public Comparator getComparator() {
        return comparator;
    }

    public long getLastRun() {
        return lastRun;
    }

    @Override
    public synchronized boolean shouldRun() {
        Profile profile = ProfileFunctions.getInstance().getProfile();
        if (profile == null)
            return false;
        IobTotal iob = IobCobCalculatorPlugin.getPlugin().calculateFromTreatmentsAndTempsSynchronized(DateUtil.now(), profile);

        if (lastRun > DateUtil.now() - T.mins(5).msecs())
            return false;

        boolean doRun = comparator.getValue().check(iob.iob, getThreshold());
        if (doRun) {
            if (L.isEnabled(L.AUTOMATION))
                log.debug("Ready for execution: " + friendlyDescription());
            return true;
        }
        return false;
    }

    @Override
    public synchronized String toJSON() {
        JSONObject o = new JSONObject();
        try {
            o.put("type", TriggerIob.class.getName());
            JSONObject data = new JSONObject();
            data.put("threshold", getThreshold());
            data.put("lastRun", lastRun);
            data.put("comparator", comparator.toString());
            o.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return o.toString();
    }

    @Override
    Trigger fromJSON(String data) {
        try {
            JSONObject d = new JSONObject(data);
            insulin.setValue(JsonHelper.safeGetInt(d, "threshold"));
            lastRun = JsonHelper.safeGetLong(d, "lastRun");
            comparator.setValue(Comparator.Compare.valueOf(JsonHelper.safeGetString(d, "comparator")));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public int friendlyName() {
        return R.string.iob;
    }

    @Override
    public String friendlyDescription() {
        return MainApp.gs(R.string.iobcompared, MainApp.gs(comparator.getValue().getStringRes()), getThreshold());
    }

    @Override
    public Optional<Integer> icon() {
        return Optional.of(R.drawable.remove); // TODO icon
    }

    @Override
    public void executed(long time) {
        lastRun = time;
    }

    @Override
    public Trigger duplicate() {
        return new TriggerIob(this);
    }

    TriggerIob threshold(double threshold) {
        insulin.setValue(threshold);
        return this;
    }

    TriggerIob lastRun(long lastRun) {
        this.lastRun = lastRun;
        return this;
    }

    TriggerIob comparator(Comparator comparator) {
        this.comparator = comparator;
        return this;
    }

    @Override
    public void generateDialog(LinearLayout root) {
        new LayoutBuilder()
                .add(comparator)
                .add(new Label(MainApp.gs(R.string.iob_u), "", insulin))
                .build(root);
    }
}
