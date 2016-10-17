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

import java.util.Optional;

import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.ban.Ban;
import org.spongepowered.api.world.World;

import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.server.player.EPlayer;
import fr.evercraft.everapi.services.sanction.Jail;
import fr.evercraft.everapi.services.sanction.Sanction.SanctionJail;
import fr.evercraft.everapi.services.sanction.Sanction.SanctionMute;
import fr.evercraft.everapi.services.sanction.SanctionService;
import fr.evercraft.everapi.sponge.UtilsNetwork;
import fr.evercraft.everapi.text.ETextBuilder;
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
			if(profile.get().isIndefinite()) {
				event.setMessage(EChat.of(ESMessages.CONNECTION_BAN_UNLIMITED.get()
						.replaceAll("<staff>", EChat.serialize(profile.get().getBanSource().orElse(Text.of(SanctionService.UNKNOWN))))
						.replaceAll("<player>", profile.get().getProfile().getName().orElse(profile.get().getProfile().getUniqueId().toString()))
						.replaceAll("<reason>", EChat.serialize(profile.get().getReason().orElse(Text.EMPTY)))
						.replaceAll("<creation_time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(creation))
						.replaceAll("<creation_date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(creation))
						.replaceAll("<creation_datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(creation))));
			} else {
				long expiration = profile.get().getExpirationDate().get().toEpochMilli();
				event.setMessage(EChat.of(ESMessages.CONNECTION_BAN_TEMP.get()
						.replaceAll("<staff>", EChat.serialize(profile.get().getBanSource().orElse(Text.of(SanctionService.UNKNOWN))))
						.replaceAll("<player>", profile.get().getProfile().getName().orElse(profile.get().getProfile().getUniqueId().toString()))
						.replaceAll("<reason>", EChat.serialize(profile.get().getReason().orElse(Text.EMPTY)))
						.replaceAll("<duration>", this.plugin.getEverAPI().getManagerUtils().getDate().formatDate(expiration))
						.replaceAll("<creation_time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(creation))
						.replaceAll("<creation_date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(creation))
						.replaceAll("<creation_datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(creation))
						.replaceAll("<expiration_time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(expiration))
						.replaceAll("<expiration_date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(expiration))
						.replaceAll("<expiration_datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(expiration))));
			}
			event.setMessageCancelled(false);
			event.setCancelled(true);
		}
		
		Optional<Ban.Ip> ip = this.plugin.getSanctionService().getBanFor(event.getConnection().getAddress().getAddress());
		if (ip.isPresent()) {
			long creation = ip.get().getCreationDate().toEpochMilli();
			if(ip.get().isIndefinite()) {
				event.setMessage(EChat.of(ESMessages.CONNECTION_BANIP_UNLIMITED.get()
						.replaceAll("<staff>", EChat.serialize(ip.get().getBanSource().orElse(Text.of(SanctionService.UNKNOWN))))
						.replaceAll("<address>", UtilsNetwork.getHostString(event.getConnection().getAddress().getAddress()))
						.replaceAll("<reason>", EChat.serialize(ip.get().getReason().orElse(Text.EMPTY)))
						.replaceAll("<creation_time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(creation))
						.replaceAll("<creation_date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(creation))
						.replaceAll("<creation_datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(creation))));
			} else {
				long expiration = ip.get().getExpirationDate().get().toEpochMilli();
				event.setMessage(EChat.of(ESMessages.CONNECTION_BANIP_TEMP.get()
						.replaceAll("<staff>", EChat.serialize(ip.get().getBanSource().orElse(Text.of(SanctionService.UNKNOWN))))
						.replaceAll("<address>", UtilsNetwork.getHostString(event.getConnection().getAddress().getAddress()))
						.replaceAll("<reason>", EChat.serialize(ip.get().getReason().orElse(Text.EMPTY)))
						.replaceAll("<duration>", this.plugin.getEverAPI().getManagerUtils().getDate().formatDate(expiration))
						.replaceAll("<creation_time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(creation))
						.replaceAll("<creation_date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(creation))
						.replaceAll("<creation_datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(creation))
						.replaceAll("<expiration_time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(expiration))
						.replaceAll("<expiration_date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(expiration))
						.replaceAll("<expiration_datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(expiration))));
			}
			event.setMessageCancelled(false);
			event.setCancelled(true);
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

		Optional<EPlayer> optPlayer = this.plugin.getEverAPI().getEServer().getEPlayer(event.getTargetEntity()); 
		
		// Joueur introuvable
		if (!optPlayer.isPresent()) {
			return;
		}
		
		EPlayer player = optPlayer.get();
		
		// Mute
		Optional<SanctionMute> optSanctionMute = player.getMute();
		if (optSanctionMute.isPresent()) {
			SanctionMute sanction = optSanctionMute.get();
			if (sanction.isIndefinite()) {
				player.sendMessage(ESMessages.PREFIX.get() + ESMessages.MUTE_CONNECTION_TEMP.get()
								.replaceAll("<staff>", sanction.getSourceName())
								.replaceAll("<reason>", EChat.serialize(sanction.getReason())
								.replaceAll("<duration>", this.plugin.getEverAPI().getManagerUtils().getDate().formatDateDiff(sanction.getCreationDate(), sanction.getExpirationDate().get()))
								.replaceAll("<creation_time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getCreationDate()))
								.replaceAll("<creation_date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getCreationDate()))
								.replaceAll("<creation_datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getCreationDate()))
								.replaceAll("<expiration_time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getExpirationDate().get()))
								.replaceAll("<expiration_date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getExpirationDate().get()))
								.replaceAll("<expiration_datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getExpirationDate().get()))));
			} else {
				player.sendMessage(ESMessages.PREFIX.get() + ESMessages.MUTE_CONNECTION_UNLIMITED.get()
								.replaceAll("<staff>", sanction.getSourceName())
								.replaceAll("<reason>", EChat.serialize(sanction.getReason())));
			}
		}
		
		// Jail
		Optional<SanctionJail> optSanctionJail = player.getJail();
		if (optSanctionJail.isPresent()) {
			SanctionJail sanction = optSanctionJail.get();
			Optional<Jail> jail = sanction.getJail();
			
			if (jail.isPresent()) {					
				if (sanction.isIndefinite()) {
					player.sendMessage(ETextBuilder.toBuilder(ESMessages.PREFIX.get())
							.append(ESMessages.JAIL_CONNECTION_TEMP.get()
									.replaceAll("<staff>", sanction.getSourceName())
									.replaceAll("<jail_name>",jail.get().getName())
									.replaceAll("<duration>", this.plugin.getEverAPI().getManagerUtils().getDate().formatDateDiff(sanction.getCreationDate(), sanction.getExpirationDate().get()))
									.replaceAll("<creation_time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getCreationDate()))
									.replaceAll("<creation_date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getCreationDate()))
									.replaceAll("<creation_datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getCreationDate()))
									.replaceAll("<expiration_time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getExpirationDate().get()))
									.replaceAll("<expiration_date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getExpirationDate().get()))
									.replaceAll("<expiration_datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getExpirationDate().get()))
									.replaceAll("<reason>", EChat.serialize(sanction.getReason())))
							.replace("<jail>", ESJail.getButtonJail(jail.get()))
							.build());
				} else {
					player.sendMessage(ETextBuilder.toBuilder(ESMessages.PREFIX.get())
							.append(ESMessages.JAIL_CONNECTION_UNLIMITED.get()
									.replaceAll("<staff>", sanction.getSourceName())
									.replaceAll("<jail_name>",jail.get().getName())
									.replaceAll("<reason>", EChat.serialize(sanction.getReason()))
									.replaceAll("<creation_time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getCreationDate()))
									.replaceAll("<creation_date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getCreationDate()))
									.replaceAll("<creation_datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getCreationDate())))
							.replace("<jail>", ESJail.getButtonJail(jail.get()))
							.build());
				}
			}
		}
	}

	/**
	 * Supprime le joueur de la liste
	 */
	@Listener
	public void onClientConnectionEvent(final ClientConnectionEvent.Disconnect event) {
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
		
		Optional<EPlayer> optPlayer = this.plugin.getEServer().getEPlayer(player_sponge);
		
		// Joueur introuvable
		if (optPlayer.isPresent()) {
			return;
		}
				
		EPlayer player = optPlayer.get();
		
		
		// Jail
		Optional<SanctionJail> optSanction = player.getJail();
		if (optSanction.isPresent()) {
			Optional<Jail> jail = optSanction.get().getJail();
			
			if (jail.isPresent()) {
				Transform<World> transform = jail.get().getTransform();
				if (!event.getToTransform().getExtent().equals(transform.getExtent()) ||
					event.getToTransform().getPosition().distance(transform.getPosition()) >= jail.get().getRadius()) {
					
					event.setToTransform(transform);
					SanctionJail sanction = optSanction.get();
					if (sanction.isIndefinite()) {
						player.sendMessage(ETextBuilder.toBuilder(ESMessages.PREFIX.get())
								.append(ESMessages.JAIL_DISABLE_MOVE_TEMP.get()
										.replaceAll("<staff>", sanction.getSourceName())
										.replaceAll("<jail_name>",jail.get().getName())
										.replaceAll("<reason>", EChat.serialize(sanction.getReason()))
										.replaceAll("<duration>", this.plugin.getEverAPI().getManagerUtils().getDate().formatDateDiff(sanction.getCreationDate(), sanction.getExpirationDate().get()))
										.replaceAll("<creation_time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getCreationDate()))
										.replaceAll("<creation_date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getCreationDate()))
										.replaceAll("<creation_datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getCreationDate()))
										.replaceAll("<expiration_time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getExpirationDate().get()))
										.replaceAll("<expiration_date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getExpirationDate().get()))
										.replaceAll("<expiration_datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getExpirationDate().get())))
								.replace("<jail>", ESJail.getButtonJail(jail.get()))
								.build());
					} else {
						player.sendMessage(ETextBuilder.toBuilder(ESMessages.PREFIX.get())
								.append(ESMessages.JAIL_DISABLE_MOVE_UNLIMITED.get()
										.replaceAll("<staff>", sanction.getSourceName())
										.replaceAll("<jail_name>",jail.get().getName())
										.replaceAll("<reason>", EChat.serialize(sanction.getReason()))
										.replaceAll("<creation_time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getCreationDate()))
										.replaceAll("<creation_date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getCreationDate()))
										.replaceAll("<creation_datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getCreationDate())))
								.replace("<jail>", ESJail.getButtonJail(jail.get()))
								.build());
					}
				}
			}
		}	
	}
	
	@Listener
    public void onPlayerWriteChat(MessageChannelEvent.Chat event, @First Player player_sponge) {
		Optional<EPlayer> optPlayer = this.plugin.getEServer().getEPlayer(player_sponge);
		if (optPlayer.isPresent()) {
			EPlayer player = optPlayer.get();
			
			// Mute
			Optional<SanctionMute> optSanction = player.getMute();
			if (optSanction.isPresent()) {
				event.setCancelled(true);
				
				SanctionMute sanction = optSanction.get();
				if (sanction.isIndefinite()) {
					player.sendMessage(ESMessages.PREFIX.get() + ESMessages.MUTE_DISABLE_CHAT_TEMP.get()
									.replaceAll("<staff>", sanction.getSourceName())
									.replaceAll("<reason>", EChat.serialize(sanction.getReason()))
									.replaceAll("<duration>", this.plugin.getEverAPI().getManagerUtils().getDate().formatDateDiff(sanction.getCreationDate(), sanction.getExpirationDate().get()))
									.replaceAll("<creation_time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getCreationDate()))
									.replaceAll("<creation_date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getCreationDate()))
									.replaceAll("<creation_datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getCreationDate()))
									.replaceAll("<expiration_time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getExpirationDate().get()))
									.replaceAll("<expiration_date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getExpirationDate().get()))
									.replaceAll("<expiration_datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getExpirationDate().get())));
				} else {
					player.sendMessage(ESMessages.PREFIX.get() + ESMessages.MUTE_DISABLE_CHAT_UNLIMITED.get()
									.replaceAll("<staff>", sanction.getSourceName())
									.replaceAll("<reason>", EChat.serialize(sanction.getReason()))
									.replaceAll("<creation_time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getCreationDate()))
									.replaceAll("<creation_date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getCreationDate()))
									.replaceAll("<creation_datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getCreationDate())));
				}
			}
		}
    }
	
	@Listener
    public void onPlayerSendCommand(SendCommandEvent event, @First Player player_sponge) {
		Optional<EPlayer> optPlayer = this.plugin.getEServer().getEPlayer(player_sponge);
		if (optPlayer.isPresent()) {
			EPlayer player = optPlayer.get();
			
			// Mute
			Optional<SanctionMute> optSanctionMute = player.getMute();
			if (optSanctionMute.isPresent() && this.plugin.getSanctionService().muteCommandsDisable(event.getCommand())) {
				event.setCancelled(true);
				
				SanctionMute sanction = optSanctionMute.get();
				if (sanction.isIndefinite()) {
					player.sendMessage(ESMessages.PREFIX.get() + ESMessages.MUTE_DISABLE_COMMAND_TEMP.get()
									.replaceAll("<staff>", sanction.getSourceName())
									.replaceAll("<reason>", EChat.serialize(sanction.getReason()))
									.replaceAll("<duration>", this.plugin.getEverAPI().getManagerUtils().getDate().formatDateDiff(sanction.getCreationDate(), sanction.getExpirationDate().get()))
									.replaceAll("<creation_time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getCreationDate()))
									.replaceAll("<creation_date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getCreationDate()))
									.replaceAll("<creation_datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getCreationDate()))
									.replaceAll("<expiration_time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getExpirationDate().get()))
									.replaceAll("<expiration_date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getExpirationDate().get()))
									.replaceAll("<expiration_datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getExpirationDate().get())));
				} else {
					player.sendMessage(ESMessages.PREFIX.get() + ESMessages.MUTE_DISABLE_COMMAND_UNLIMITED.get()
									.replaceAll("<staff>", sanction.getSourceName())
									.replaceAll("<reason>", EChat.serialize(sanction.getReason()))
									.replaceAll("<creation_time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getCreationDate()))
									.replaceAll("<creation_date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getCreationDate()))
									.replaceAll("<creation_datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getCreationDate())));
				}
			}
			
			Optional<SanctionJail> optSanctionJail = player.getJail();
			if (optSanctionJail.isPresent() && !this.plugin.getSanctionService().jailCommandsEnable(event.getCommand())) {
				SanctionJail sanction = optSanctionJail.get();
				Optional<Jail> jail = sanction.getJail();
				
				if (jail.isPresent()) {
					event.setCancelled(true);
					
					if (sanction.isIndefinite()) {
						player.sendMessage(ETextBuilder.toBuilder(ESMessages.PREFIX.get())
								.append(ESMessages.JAIL_DISABLE_COMMAND_TEMP.get()
										.replaceAll("<staff>", sanction.getSourceName())
										.replaceAll("<jail_name>",jail.get().getName())
										.replaceAll("<reason>", EChat.serialize(sanction.getReason()))
										.replaceAll("<duration>", this.plugin.getEverAPI().getManagerUtils().getDate().formatDateDiff(sanction.getCreationDate(), sanction.getExpirationDate().get()))
										.replaceAll("<creation_time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getCreationDate()))
										.replaceAll("<creation_date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getCreationDate()))
										.replaceAll("<creation_datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getCreationDate()))
										.replaceAll("<expiration_time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getExpirationDate().get()))
										.replaceAll("<expiration_date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getExpirationDate().get()))
										.replaceAll("<expiration_datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getExpirationDate().get())))
								.replace("<jail>", ESJail.getButtonJail(jail.get()))
								.build());
					} else {
						player.sendMessage(ETextBuilder.toBuilder(ESMessages.PREFIX.get())
								.append(ESMessages.JAIL_DISABLE_COMMAND_UNLIMITED.get()
										.replaceAll("<staff>", sanction.getSourceName())
										.replaceAll("<reason>", EChat.serialize(sanction.getReason()))
										.replaceAll("<jail_name>",jail.get().getName())
										.replaceAll("<creation_time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getCreationDate()))
										.replaceAll("<creation_date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getCreationDate()))
										.replaceAll("<creation_datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getCreationDate())))
								.replace("<jail>", ESJail.getButtonJail(jail.get()))
								.build());
					}
				}
			}
		}
    }
}
