package org.workcraft.plugins.mpsat;

import org.workcraft.plugins.mpsat.MpsatSettings.SolutionMode;
import org.workcraft.plugins.shared.presets.Preset;

public class MpsatBuiltinPresets {

	public static final Preset<MpsatSettings> DEADLOCK_CHECKER = new Preset<MpsatSettings>(
			"Deadlock (shortest trace)", new MpsatSettings(MpsatMode.DEADLOCK, 0,
					SolutionMode.MINIMUM_COST, 0, null), true);

	public static final Preset<MpsatSettings> DEADLOCK_CHECKER_ALL_TRACES = new Preset<MpsatSettings>(
			"Deadlock (all traces)", new MpsatSettings(MpsatMode.DEADLOCK, 0,
					SolutionMode.ALL, 0, null), true);

	public static final Preset<MpsatSettings> CONSISTENCY_CHECKER = new Preset<MpsatSettings>(
			"Consistency", new MpsatSettings(MpsatMode.STG_REACHABILITY, 0,
					SolutionMode.MINIMUM_COST, 0, MpsatSettings.reachConsistency), true);

	public static final Preset<MpsatSettings> PERSISTENCY_CHECKER = new Preset<MpsatSettings>(
			"Output persistency (without dummies)", new MpsatSettings(MpsatMode.STG_REACHABILITY, 0,
					SolutionMode.MINIMUM_COST, 0, MpsatSettings.reachSemimodularity), true);

}
