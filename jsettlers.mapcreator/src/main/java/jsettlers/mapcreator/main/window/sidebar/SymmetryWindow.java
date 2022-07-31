package jsettlers.mapcreator.main.window.sidebar;

import jsettlers.common.utils.mutables.Mutable;
import jsettlers.graphics.localization.Labels;
import jsettlers.mapcreator.data.SymmetryConfig;
import jsettlers.mapcreator.tools.shapes.ShapeIcon;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;

public class SymmetryWindow extends JPanel {

	public SymmetryWindow() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	}

	public static JComponent createOverview(Mutable<SymmetryConfig> symmetry) {
		JToolBar symInfo = new JToolBar();
		symInfo.setFloatable(false);

		ButtonGroup symGroup = new ButtonGroup();

		JButton noSym = new JButton(ShapeIcon.NO_SYM);
		noSym.addActionListener(l -> {
			symmetry.object = SymmetryConfig.DEFAULT;
		});

		symGroup.add(noSym);
		symInfo.add(noSym);

		JButton openSymPanel = new JButton(Labels.getString("symmetry.open"));
		openSymPanel.addActionListener(l -> {
			SymmetryWindow sym = new SymmetryWindow();
		});

		symGroup.add(openSymPanel);
		symInfo.add(openSymPanel);
		return symInfo;
	}
}
