/*
 * This file is part of EverSanctions.
 *
 * EverSanctions is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EverSanctions is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with EverSanctions.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.evercraft.eversanctions;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.ban.Ban;
import org.spongepowered.api.world.World;

import fr.evercraft.everapi.message.replace.EReplace;
import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.server.player.EPlayer;
import fr.evercraft.everapi.services.sanction.Sanction;
import fr.evercraft.everapi.services.sanction.Sanction.SanctionJail;
import fr.evercraft.everapi.services.sanction.Sanction.SanctionMute;
import fr.evercraft.everapi.services.jail.Jail;
import fr.evercraft.everapi.services.sanction.SanctionService;
import fr.evercraft.everapi.sponge.UtilsNetwork;
import fr.evercraft.eversanctions.ESMessage.ESMessages;
import fr.evercraft.eversanctions.command.jail.ESJail;

public class ESListener {	
	private EverSanctions plugin;
	
	public ESListener(final EverSanctions plugin) {
		this.plugin = plugin;
	}
	
	/**
	 * Ajoute le joueur dans le cache
	 */
	@Listener(order = Order.FIRST)
	public void onClientConnectionEvent(final ClientConnectionEvent.Auth event) {
		Optional<Ban.Profile> profile = this.plugin.getSanctionService().getBanFor(event.getProfile());
		if (profile.isPresent()) {
			long creation = profile.get().getCreationDate().toEpochMilli();
			Map<String, EReplace<?>> replaces = new HashMap<String, EReplace<?>>();
			replaces.put("<staff>", EReplace.of(Sanction.getSourceName(profile.get().getBanSource(), this.plugin.getEServer())));
			replaces.put("<player>", EReplace.of(profile.get().getProfile().getName().orElse(profile.get().getProfile().getUniqueId().toString())));
			replaces.put("<reason>", EReplace.of(EChat.serialize(profile.get().getReason().orElse(Text.EMPTY))));
			replaces.put("<creation_time>", EReplace.of(() -> this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(creation)));
			replaces.put("<creation_date>", EReplace.of(() -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(creation)));
			replaces.put("<creation_datetime>", EReplace.of(() -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(creation)));
			
			if(profile.get().isIndefinite()) {				
				event.setMessage(ESMessages.CONNECTION_BAN_UNLIMITED.getFormat().toText2(replaces));
			} else {
				long expiration = profile.get().getExpirationDate().get().toEpochMilli();
				this.plugin.getEServer().broadcast("Test : ");
				this.plugin.getEServer().broadcast("" + creation);
				this.plugin.getEServer().broadcast("" + expiration);
				replaces.put("<duration>", EReplace.of(() -> this.plugin.getEverAPI().getManagerUtils().getDate().formatDateDiff(creation, expiration)));
				replaces.put("<expiration_time>", EReplace.of(() -> this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(expiration)));
				replaces.put("<expiration_date>", EReplace.of(() -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(expiration)));
				replaces.put("<expiration_datetime>", EReplace.of(() -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(expiration)));
				
				event.setMessage(ESMessages.CONNECTION_BAN_TEMP.getFormat().toText2(replaces));
			}
			event.setMessageCancelled(false);
			event.setCancelled(true);
			return;
		}
		
		Optional<Ban.Ip> ip = this.plugin.getSanctionService().getBanFor(event.getConnection().getAddress().getAddress());
		if (ip.isPresent()) {
			long creation = ip.get().getCreationDate().toEpochMilli();
			Map<String, EReplace<?>> replaces = new HashMap<String, EReplace<?>>();
			replaces.put("<staff>", EReplace.of(Sanction.getSourceName(ip.get().getBanSource(), this.plugin.getEServer())));
			replaces.put("<address>", EReplace.of(UtilsNetwork.getHostString(event.getConnection().getAddress().getAddress())));
			replaces.put("<reason>", EReplace.of(EChat.serialize(ip.get().getReason().orElse(Text.EMPTY))));
			replaces.put("<creation_time>", EReplace.of(() -> this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(creation)));
			replaces.put("<creation_date>", EReplace.of(() -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(creation)));
			replaces.put("<creation_datetime>", EReplace.of(() -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(creation)));
			
			if(ip.get().isIndefinite()) {
				event.setMessage(ESMessages.CONNECTION_BANIP_UNLIMITED.getFormat().toText2(replaces));
			} else {
				long expiration = ip.get().getExpirationDate().get().toEpochMilli();
				replaces.put("<duration>", EReplace.of(() -> this.plugin.getEverAPI().getManagerUtils().getDate().formatDateDiff(creation, expiration)));
				replaces.put("<expiration_time>", EReplace.of(() -> this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(expiration)));
				replaces.put("<expiration_date>", EReplace.of(() -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(expiration)));
				replaces.put("<expiration_datetime>", EReplace.of(() -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(expiration)));
				
				event.setMessage(ESMessages.CONNECTION_BANIP_TEMP.getFormat().toText2(replaces));
			}
			event.setMessageCancelled(false);
			event.setCancelled(true);
			return;
		}
		
		if (!event.isCancelled()) {
			this.plugin.getSanctionService().get(event.getProfile().getUniqueId());
		}
	}

	/**
	 * Ajoute le joueur à la liste
	 */
	@Listener
	public void onClientConnectionEvent(final ClientConnectionEvent.Join event, @Getter("getTargetEntity") Player player_sponge) {
		this.plugin.getSanctionService().registerPlayer(player_sponge.getUniqueId());

		EPlayer player = this.plugin.getEverAPI().getEServer().getEPlayer(event.getTargetEntity());
		
		// Mute
		Optional<SanctionMute> optSanctionMute = player.getMute();
		if (optSanctionMute.isPresent()) {
			SanctionMute sanction = optSanctionMute.get();
			if (sanction.isIndefinite()) {
				ESMessages.MUTE_CONNECTION_UNLIMITED.sender()
					.replace("<staff>", sanction.getSourceName(this.plugin.getEServer()))
					.replace("<reason>", sanction.getReason())
					.sendTo(player);
			} else {
				ESMessages.MUTE_CONNECTION_TEMP.sender()
					.replace("<staff>", sanction.getSourceName(this.plugin.getEServer()))
					.replace("<reason>", sanction.getReason())
					.replace("<duration>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().formatDateDiff(sanction.getCreationDate(), sanction.getExpirationDate().get()))
					.replace("<creation_time>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getCreationDate()))
					.replace("<creation_date>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getCreationDate()))
					.replace("<creation_datetime>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getCreationDate()))
					.replace("<expiration_time>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getExpirationDate().get()))
					.replace("<expiration_date>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getExpirationDate().get()))
					.replace("<expiration_datetime>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getExpirationDate().get()))
					.sendTo(player);
			}
		}
		
		// Jail
		Optional<SanctionJail> optSanctionJail = player.getJail();
		if (optSanctionJail.isPresent()) {
			SanctionJail sanction = optSanctionJail.get();
			Optional<Jail> jail = sanction.getJail();
			
			if (jail.isPresent()) {	
				player.setTransform(jail.get().getTransform());
				if (sanction.isIndefinite()) {
					ESMessages.JAIL_CONNECTION_UNLIMITED.sender()
						.replace("<staff>", sanction.getSourceName(this.plugin.getEServer()))
						.replace("<jail_name>",jail.get().getName())
						.replace("<reason>", sanction.getReason())
						.replace("<creation_time>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getCreationDate()))
						.replace("<creation_date>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getCreationDate()))
						.replace("<creation_datetime>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getCreationDate()))
						.replace("<jail>", () -> ESJail.getButtonJail(jail.get()))
						.sendTo(player);
				} else {
					ESMessages.JAIL_CONNECTION_TEMP.sender()
						.replace("<staff>", sanction.getSourceName(this.plugin.getEServer()))
						.replace("<jail_name>",jail.get().getName())
						.replace("<duration>", this.plugin.getEverAPI().getManagerUtils().getDate().formatDateDiff(sanction.getCreationDate(), sanction.getExpirationDate().get()))
						.replace("<creation_time>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getCreationDate()))
						.replace("<creation_date>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getCreationDate()))
						.replace("<creation_datetime>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getCreationDate()))
						.replace("<expiration_time>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getExpirationDate().get()))
						.replace("<expiration_date>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getExpirationDate().get()))
						.replace("<expiration_datetime>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getExpirationDate().get()))
						.replace("<reason>", sanction.getReason())
						.replace("<jail>", () -> ESJail.getButtonJail(jail.get()))
						.sendTo(player);
				}
			}
		}
	}

	/**
	 * Supprime le joueur de la liste
	 */
	@Listener
	public void onClientConnectionEvent(final ClientConnectionEvent.Disconnect event, @Getter("getTargetEntity") Player player_sponge) {
		EPlayer player = this.plugin.getEverAPI().getEServer().getEPlayer(event.getTargetEntity());
			
		// Jail
		Optional<SanctionJail> optSanction = player.getJail();
		if (optSanction.isPresent()) {
			Optional<Jail> jail = optSanction.get().getJail();
			if (jail.isPresent() && player.getBack().isPresent()) {
				player.setTransform(player.getBack().get());
			}
		}
		
		this.plugin.getSanctionService().removePlayer(event.getTargetEntity().getUniqueId());
	}
	
	@Listener
	public void onPlayerMove(MoveEntityEvent event, @Getter("getTargetEntity") Player player_sponge) {
		// Même bloc
		if (event.getFromTransform().getExtent().equals(event.getToTransform().getExtent()) &&
				Math.ceil(event.getFromTransform().getPosition().getX()) == Math.ceil(event.getToTransform().getPosition().getX()) &&
				Math.ceil(event.getFromTransform().getPosition().getY()) == Math.ceil(event.getToTransform().getPosition().getY()) &&
				Math.ceil(event.getFromTransform().getPosition().getZ()) == Math.ceil(event.getToTransform().getPosition().getZ())) {
			return;
		}
		
		EPlayer player = this.plugin.getEverAPI().getEServer().getEPlayer(player_sponge);
		
		// Jail
		Optional<SanctionJail> optSanction = player.getJail();
		if (optSanction.isPresent()) {
			Optional<Jail> jail = optSanction.get().getJail();
			
			if (jail.isPresent()) {
				Transform<World> transform = jail.get().getTransform();
				if (!event.getToTransform().getExtent().equals(transform.getExtent()) ||
					event.getToTransform().getPosition().distance(transform.getPosition()) >= jail.get().getRadius()) {
					
					event.setToTransform(transform);
					player.sendActionBar(SanctionService.MESSAGE_JAIL, 3, ESMessages.JAIL_DISABLE_MOVE.getText());
				}
			}
		}	
	}
	
	@Listener
	public void onPlayerInteractEntity(InteractEntityEvent event, @First Player player) {
		if (event.isCancelled()) {
			return;
		}
		event.setCancelled(this.jailInteract(player));
	}
	
	@Listener
	public void onPlayerInteractEntity(ChangeBlockEvent event, @First Player player) {
		if (event.isCancelled()) {
			return;
		}
		event.setCancelled(this.jailInteract(player));
	}
	
	@Listener
	public void onPlayerInteractEntity(InteractBlockEvent event, @First Player player) {
		if (event.isCancelled()) {
			return;
		}
		event.setCancelled(this.jailInteract(player));
	}
	
	public boolean jailInteract(Player player_sponge) {		
		EPlayer player = this.plugin.getEverAPI().getEServer().getEPlayer(player_sponge);
		
		// Jail
		Optional<SanctionJail> optSanction = player.getJail();
		if (!optSanction.isPresent()) {
			return false;
		}
		
		Optional<Jail> jail = optSanction.get().getJail();
		if (!jail.isPresent()) {
			return false;
		}
		
		ESMessages.JAIL_DISABLE_INTERACT.sendTo(player);
		return true;
	}
	
	@Listener
    public void onPlayerWriteChat(MessageChannelEvent.Chat event, @First Player player_sponge) {
		EPlayer player = this.plugin.getEverAPI().getEServer().getEPlayer(player_sponge);
			
		// Mute
		Optional<SanctionMute> optSanction = player.getMute();
		if (optSanction.isPresent()) {
			event.setCancelled(true);
			
			SanctionMute sanction = optSanction.get();
			if (sanction.isIndefinite()) {
				ESMessages.MUTE_DISABLE_CHAT_TEMP.sender()
					.replace("<staff>", sanction.getSourceName(this.plugin.getEServer()))
					.replace("<reason>", sanction.getReason())
					.replace("<duration>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().formatDateDiff(sanction.getCreationDate(), sanction.getExpirationDate().get()))
					.replace("<creation_time>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getCreationDate()))
					.replace("<creation_date>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getCreationDate()))
					.replace("<creation_datetime>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getCreationDate()))
					.replace("<expiration_time>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getExpirationDate().get()))
					.replace("<expiration_date>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getExpirationDate().get()))
					.replace("<expiration_datetime>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getExpirationDate().get()))
					.sendTo(player);
			} else {
				ESMessages.MUTE_DISABLE_CHAT_UNLIMITED.sender()
					.replace("<staff>", sanction.getSourceName(this.plugin.getEServer()))
					.replace("<reason>", sanction.getReason())
					.replace("<creation_time>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getCreationDate()))
					.replace("<creation_date>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getCreationDate()))
					.replace("<creation_datetime>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getCreationDate()))
					.sendTo(player);
			}
		}
    }
	
	@Listener
    public void onPlayerSendCommand(SendCommandEvent event, @First Player player_sponge) {
		EPlayer player = this.plugin.getEverAPI().getEServer().getEPlayer(player_sponge);
			
		// Mute
		Optional<SanctionMute> optSanctionMute = player.getMute();
		if (optSanctionMute.isPresent() && this.plugin.getSanctionService().muteCommandsDisable(event.getCommand())) {
			event.setCancelled(true);
			
			SanctionMute sanction = optSanctionMute.get();
			if (sanction.isIndefinite()) {
				ESMessages.MUTE_DISABLE_COMMAND_TEMP.sender()
					.replace("<staff>", sanction.getSourceName(this.plugin.getEServer()))
					.replace("<reason>", sanction.getReason())
					.replace("<duration>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().formatDateDiff(sanction.getCreationDate(), sanction.getExpirationDate().get()))
					.replace("<creation_time>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getCreationDate()))
					.replace("<creation_date>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getCreationDate()))
					.replace("<creation_datetime>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getCreationDate()))
					.replace("<expiration_time>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getExpirationDate().get()))
					.replace("<expiration_date>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getExpirationDate().get()))
					.replace("<expiration_datetime>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getExpirationDate().get()))
					.sendTo(player);
			} else {
				ESMessages.MUTE_DISABLE_COMMAND_UNLIMITED.sender()
					.replace("<staff>", sanction.getSourceName(this.plugin.getEServer()))
					.replace("<reason>", sanction.getReason())
					.replace("<creation_time>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getCreationDate()))
					.replace("<creation_date>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getCreationDate()))
					.replace("<creation_datetime>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getCreationDate()))
					.sendTo(player);
			}
		}
		
		Optional<SanctionJail> optSanctionJail = player.getJail();
		if (optSanctionJail.isPresent() && !this.plugin.getSanctionService().jailCommandsEnable(event.getCommand())) {
			SanctionJail sanction = optSanctionJail.get();
			Optional<Jail> jail = sanction.getJail();
			
			if (jail.isPresent()) {
				event.setCancelled(true);
				
				if (sanction.isIndefinite()) {
					ESMessages.JAIL_DISABLE_COMMAND_TEMP.sender()
						.replace("<staff>", sanction.getSourceName(this.plugin.getEServer()))
						.replace("<jail_name>",jail.get().getName())
						.replace("<reason>", sanction.getReason())
						.replace("<duration>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().formatDateDiff(sanction.getCreationDate(), sanction.getExpirationDate().get()))
						.replace("<creation_time>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getCreationDate()))
						.replace("<creation_date>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getCreationDate()))
						.replace("<creation_datetime>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getCreationDate()))
						.replace("<expiration_time>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getExpirationDate().get()))
						.replace("<expiration_date>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getExpirationDate().get()))
						.replace("<expiration_datetime>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getExpirationDate().get()))
						.replace("<jail>", () -> ESJail.getButtonJail(jail.get()))
						.sendTo(player);
				} else {
					ESMessages.JAIL_DISABLE_COMMAND_UNLIMITED.sender()
						.replace("<staff>", sanction.getSourceName(this.plugin.getEServer()))
						.replace("<reason>", sanction.getReason())
						.replace("<jail_name>",jail.get().getName())
						.replace("<creation_time>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getCreationDate()))
						.replace("<creation_date>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getCreationDate()))
						.replace("<creation_datetime>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getCreationDate()))
						.replace("<jail>", () -> ESJail.getButtonJail(jail.get()))
						.sendTo(player);
				}
			}
		}
    }
}
