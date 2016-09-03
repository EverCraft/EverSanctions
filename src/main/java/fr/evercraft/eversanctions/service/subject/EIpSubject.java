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
package fr.evercraft.eversanctions.service.subject;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.annotation.Nullable;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import fr.evercraft.everapi.exception.ServerDisableException;
import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.services.sanction.SubjectIpSanction;
import fr.evercraft.everapi.services.sanction.manual.SanctionManualIP;
import fr.evercraft.eversanctions.EverSanctions;
import fr.evercraft.eversanctions.service.manual.EManualIP;

public class EIpSubject implements SubjectIpSanction {
	
	private final EverSanctions plugin;
	private final InetAddress address;
	
	private final ConcurrentSkipListSet<EManualIP> manual_expiry;
	private Optional<EManualIP> manual;

	public EIpSubject(final EverSanctions plugin, final InetAddress address) {
		this.plugin = plugin;
		this.address = address;
		
		this.manual_expiry = new ConcurrentSkipListSet<EManualIP>((EManualIP o1, EManualIP o2) -> o2.getCreationDate().compareTo(o1.getCreationDate()));
		this.manual = Optional.empty();
		
		this.reload();
	}
	
	public void reload() {
		this.manual_expiry.clear();
		this.manual = Optional.empty();
		
		this.manual_expiry.addAll(this.selectSQL());
		
		EManualIP last = this.manual_expiry.first();
		if (last != null && !last.isExpire()) {
			this.manual = Optional.of(last);
			this.manual_expiry.remove(last);
		}
	}
	
	public Collection<SanctionManualIP> getAll() {
		Builder<SanctionManualIP> builder = new ImmutableList.Builder<SanctionManualIP>().addAll(this.manual_expiry);
		if(this.manual.isPresent()) {
			builder = builder.add(this.manual.get());
		}
		return builder.build();
	}

	@Override
	public boolean isBan() {
		return this.manual.isPresent();
	}
	
	@Override
	public boolean add(final Long creation, final Text reason, final String source) {
		return this.add(creation, null, reason, source);
	}

	@Override
	public boolean add(final Long creation, @Nullable final Long duration, final Text reason, final String source) {
		Preconditions.checkNotNull(creation, "creation");
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");
		
		if(!this.isBan()) {
			final EManualIP ban = new EManualIP(creation, duration, reason, source);
			if(!Sponge.getEventManager().post(SpongeEventFactory.createBanIpEvent(Cause.source(this).build(), ban.getBan(this.address)))) {
				this.manual = Optional.of(ban);
				this.plugin.getThreadAsync().execute(() -> this.addSQL(ban));
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean pardon(final Long date, final Text reason, final String source) {
		Preconditions.checkNotNull(date, "date");
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");
		
		if(this.manual.isPresent()) {
			final EManualIP ban = this.manual.get();
			this.plugin.getThreadAsync().execute(() -> this.pardonSQL(ban));
			
		}
		return false;
	}
	
	public boolean remove(final SanctionManualIP ban) {
		Preconditions.checkNotNull(ban, "ban");
		
		if(this.manual.isPresent() && this.manual.get().equals(ban)) {
			this.plugin.getThreadAsync().execute(() -> this.removeSQL(ban));
			this.manual = Optional.empty();
			return true;
		}
		
		if(this.manual_expiry.contains(ban)) {
			this.plugin.getThreadAsync().execute(() -> this.removeSQL(ban));
			this.manual_expiry.remove(ban);
			return true;
		}
		
		return false;
	}
	
	public boolean clear() {
		if(!this.getAll().isEmpty()) {
			this.manual_expiry.clear();
			this.manual = Optional.empty();
			
			this.plugin.getThreadAsync().execute(() -> this.clearSQL());
			return true;
		}
		return false;
	}
	
	/*
	 * Assesseur
	 */
	
	public InetAddress getAddress() {
		return this.address;
	}
	
	/*
	 * Database
	 */
	
	private Collection<EManualIP> selectSQL() {
		Collection<EManualIP> ips = new ArrayList<EManualIP>();
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		try {
			connection = this.plugin.getDataBase().getConnection();
			String query = "SELECT * "
						+ "FROM `" + this.plugin.getDataBase().getTableManualIp() + "` "
						+ "WHERE `identifier` = ? ;";
			preparedStatement = this.plugin.getDataBase().getConnection().prepareStatement(query);
			preparedStatement.setString(1, this.address.getHostAddress());
			ResultSet list = preparedStatement.executeQuery();
			while(list.next()) {
				Long duration = list.getLong("duration");
				if(list.wasNull()) {
					duration = null;
				}
				
				ips.add(new EManualIP(list.getTimestamp("creation").getTime(), 
										duration, 
										EChat.of(list.getString("reason")),
										list.getString("source")));
			}
		} catch (SQLException e) {
	    	this.plugin.getLogger().warn("Error during a change of manual_ip : (identifier:'" + this.address.getHostAddress() + "'): " + e.getMessage());
		} catch (ServerDisableException e) {
			e.execute();
		} finally {
			try {
				if (preparedStatement != null) preparedStatement.close();
				if (connection != null) connection.close();
			} catch (SQLException e) {}
	    }
		return ips;
	}
	
	private void addSQL(final SanctionManualIP ban) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		
    	try {    		
    		connection = this.plugin.getDataBase().getConnection();
    		String query = 	  "INSERT INTO `" + this.plugin.getDataBase().getTableManualProfile() + "` "
    						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, this.address.getHostAddress());
			preparedStatement.setTimestamp(2, new Timestamp(ban.getCreationDate()));
			preparedStatement.setLong(3, ban.getDuration().orElse(null));
			preparedStatement.setString(4, EChat.serialize(ban.getReason()));
			preparedStatement.setString(5, ban.getSource());
			
			if(ban.isPardon()) {
				preparedStatement.setTimestamp(6, new Timestamp(ban.getPardonDate().get()));
				preparedStatement.setString(7, EChat.serialize(ban.getPardonReason().get()));
				preparedStatement.setString(8, ban.getPardonSource().get());
			} else {
				preparedStatement.setTimestamp(6, null);
				preparedStatement.setString(7, null);
				preparedStatement.setString(8, null);
			}
			preparedStatement.execute();
			this.plugin.getLogger().debug("Adding to the database : (identifier ='" + this.address.getHostAddress() + "';"
					 											  + "creation='" + ban.getCreationDate() + "';"
					 											  + "duration='" + ban.getDuration().orElse(-1L) + "';"
					 											  + "reason='" + EChat.serialize(ban.getReason()) + "';"
					 											  + "source='" + ban.getCreationDate() + "';"
					 											  + "pardon_date='" + ban.getPardonDate().orElse(-1L) + "';"
					 											  + "pardon_reason='" + ban.getPardonReason().orElse(Text.EMPTY) + "';"
																  + "pardon_source='" + ban.getPardonSource().orElse("null") + "')");
    	} catch (SQLException e) {
        	this.plugin.getLogger().warn("Error during a change of manual : " + e.getMessage());
		} catch (ServerDisableException e) {
			e.execute();
		} finally {
			try {
				if (preparedStatement != null) preparedStatement.close();
				if (connection != null) connection.close();
			} catch (SQLException e) {}
	    }
	}
	
	private void pardonSQL(final SanctionManualIP ban) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		
    	try {    		
    		connection = this.plugin.getDataBase().getConnection();
    		String query = 	  "UPDATE `" + this.plugin.getDataBase().getTableManualProfile() + "` "
    						+ "SET pardon_date = ? ,"
    							+ "pardon_reason = ? ,"
    							+ "pardon_source = ? "
    						+ "WHERE uuid = ? AND creation = ? ;";
    		preparedStatement = connection.prepareStatement(query);

			if(ban.isPardon()) {
				preparedStatement.setTimestamp(1, new Timestamp(ban.getPardonDate().get()));
				preparedStatement.setString(2, EChat.serialize(ban.getPardonReason().get()));
				preparedStatement.setString(3, ban.getPardonSource().get());
			} else {
				preparedStatement.setTimestamp(1, null);
				preparedStatement.setString(2, null);
				preparedStatement.setString(3, null);
			}
			preparedStatement.setString(4, this.address.getHostAddress());
			preparedStatement.setTimestamp(5, new Timestamp(ban.getCreationDate()));
			preparedStatement.execute();
			this.plugin.getLogger().debug("Updating to the database : (identifier='" + this.address.getHostAddress() + "';"
					 											  + "creation='" + ban.getCreationDate() + "';"
					 											  + "pardon_date='" + ban.getPardonDate().orElse(-1L) + "';"
					 											  + "pardon_reason='" + ban.getPardonReason().orElse(Text.EMPTY) + "';"
																  + "pardon_source='" + ban.getPardonSource().orElse("") + "')");
    	} catch (SQLException e) {
        	this.plugin.getLogger().warn("Error during a change of manual : " + e.getMessage());
		} catch (ServerDisableException e) {
			e.execute();
		} finally {
			try {
				if (preparedStatement != null) preparedStatement.close();
				if (connection != null) connection.close();
			} catch (SQLException e) {}
	    }
	}
	
	private void removeSQL(final SanctionManualIP ban) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
    	try {
    		connection = this.plugin.getDataBase().getConnection();
    		String query = 	  "DELETE " 
		    				+ "FROM `" + this.plugin.getDataBase().getTableManualIp() + "` "
		    				+ "WHERE `identifier` = ? AND `creation` = ? ;";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, this.address.getHostAddress());
			preparedStatement.setTimestamp(2, new Timestamp(ban.getCreationDate()));
			
			preparedStatement.execute();
			this.plugin.getLogger().debug("Remove from database : (identifier='" + this.address.getHostAddress() + "';"
					 											  + "creation='" + ban.getCreationDate() + "';"
					 											  + "duration='" + ban.getDuration().orElse(-1L) + "';"
					 											  + "reason='" + EChat.serialize(ban.getReason()) + "';"
					 											  + "source='" + ban.getCreationDate() + "';"
					 											  + "pardon_date='" + ban.getPardonDate().orElse(-1L) + "';"
					 											  + "pardon_reason='" + ban.getPardonReason().orElse(Text.EMPTY) + "';"
																  + "pardon_source='" + ban.getPardonSource().orElse("null") + "')");
    	} catch (SQLException e) {
        	this.plugin.getLogger().warn("Error during a change of manual : " + e.getMessage());
		} catch (ServerDisableException e) {
			e.execute();
		} finally {
			try {
				if (preparedStatement != null) preparedStatement.close();
				if (connection != null) connection.close();
			} catch (SQLException e) {}
	    }
	}
	
	private void clearSQL() {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
    	try {
    		connection = this.plugin.getDataBase().getConnection();
    		String query = 	  "DELETE " 
		    				+ "FROM `" + this.plugin.getDataBase().getTableManualProfile() + "` "
		    				+ "WHERE `identifier` = ? ;";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, this.address.getHostAddress());
			
			preparedStatement.execute();
			this.plugin.getLogger().debug("Remove from database : (identifier='" + this.address.getHostAddress() + "';");
    	} catch (SQLException e) {
        	this.plugin.getLogger().warn("Error during a change of manual : " + e.getMessage());
		} catch (ServerDisableException e) {
			e.execute();
		} finally {
			try {
				if (preparedStatement != null) preparedStatement.close();
				if (connection != null) connection.close();
			} catch (SQLException e) {}
	    }
	}
}
