package jsettlers.main.android.mainmenu.home;

import android.arch.lifecycle.ViewModelProviders;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import jsettlers.main.android.R;
import jsettlers.main.android.databinding.VhMultiPlayerBinding;
import jsettlers.main.android.mainmenu.navigation.MainMenuNavigator;

public class MultiPlayerViewHolder extends RecyclerView.ViewHolder {

	private final MainMenuNavigator mainMenuNavigator;

	public MultiPlayerViewHolder(View itemView, Fragment parent, MainMenuNavigator mainMenuNavigator) {
		super(itemView);
		this.mainMenuNavigator = mainMenuNavigator;

		MainMenuViewModel viewModel = ViewModelProviders.of(parent).get(MainMenuViewModel.class);
		VhMultiPlayerBinding binding = VhMultiPlayerBinding.bind(itemView);
		binding.setLifecycleOwner(parent);
		binding.setViewmodel(viewModel);

		TextView connectionStatus = itemView.findViewById(R.id.text_view_connection_status);
		Button connectButton = itemView.findViewById(R.id.button_start_server);

		viewModel.getShowMultiplayerPlayer().observe(parent, z -> mainMenuNavigator.showNewMultiPlayerPicker());
		viewModel.getShowJoinMultiplayerPlayer().observe(parent, z -> mainMenuNavigator.showJoinMultiPlayerPicker());
		viewModel.getMultiplayerStatus().observe(parent, z -> {
			if(z == Boolean.FALSE) {
				connectButton.setText(R.string.menu_multi_player_start_server);

				connectionStatus.setText("");
			} else {
				String ip = null;
				try {
					Enumeration<NetworkInterface> netIter = NetworkInterface.getNetworkInterfaces();
					while(netIter.hasMoreElements() || ip != null) {
						NetworkInterface net = netIter.nextElement();
						if(net.isLoopback()) continue;
						if(!net.isUp()) continue;

						Enumeration<InetAddress> addresses = net.getInetAddresses();
						while(addresses.hasMoreElements()) {
							InetAddress address = addresses.nextElement();

							if(address instanceof Inet4Address) {
								ip = address.getHostAddress();
								break;
							}
						}
					}
				} catch (SocketException e) {
					e.printStackTrace();
				}

				if(ip == null) ip = "unknown";

				connectionStatus.setText("Server running (ip: " + ip + ")");

				connectButton.setText(R.string.menu_multi_player_stop_server);
			}
		});
	}
}
