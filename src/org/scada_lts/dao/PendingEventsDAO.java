/*
 * (c) 2015 Abil'I.T. http://abilit.eu/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.scada_lts.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.scada_lts.dao.model.UserCommentCache;
import org.scada_lts.utils.ApplicationBeans;
import org.scada_lts.utils.EventTypeUtil;


import com.serotonin.mango.rt.event.EventInstance;
import com.serotonin.mango.rt.event.type.EventType;
import com.serotonin.mango.vo.UserComment;
import com.serotonin.web.i18n.LocalizableMessage;
import com.serotonin.web.i18n.LocalizableMessageParseException;

/**
 * DAO for Pending Events.
 *
 * @author grzegorz bylica Abil'I.T. development team, sdt@abilit.eu
 * person supporting and coreecting translation Jerzy Piejko
 */
public class PendingEventsDAO {

	private static final Log LOG = LogFactory.getLog(PendingEventsDAO.class);

	private final static String  COLUMN_NAME_EVENT_ID = "id";
	private final static String  COLUMN_NAME_EVENT_TYPE_ID = "typeId";
	private final static String  COLUMN_NAME_EVENT_TYPE_REF1 = "typeRef1";
	private final static String  COLUMN_NAME_EVENT_TYPE_REF2 = "typeRef2";
	private final static String  COLUMN_NAME_EVENT_ACTIVE_TS = "activeTs";
	private final static String  COLUMN_NAME_EVENT_RTN_APPLICABLE = "rtnApplicable";
	private final static String  COLUMN_NAME_EVENT_RTN_TS = "rtnTs";
	private final static String  COLUMN_NAME_EVENT_RTN_COUSE = "rtnCause";
	private final static String  COLUMN_NAME_EVENT_ALARM_LEVEL = "alarmLevel";
	private final static String  COLUMN_NAME_EVENT_MESSAGE = "message";
	private final static String  COLUMN_NAME_EVENT_SHORT_MESSAGE = "shortMessage";
	private final static String  COLUMN_NAME_EVENT_ACK_TS = "ackTs";
	private final static String  COLUMN_NAME_EVENT_ACK_USER_ID = "ackUserId";
	private final static String  COLUMN_NAME_EVENT_USERNAME = "username";
	private final static String  COLUMN_NAME_EVENT_ALTERNATE_ACK_SOURCE = "alternateAckSource";
	private final static String  COLUMN_NAME_EVENT_SILENCED = "silenced";

	private final static String  COLUMN_NAME_COMMENT_USER_ID = "userId";
	private final static String  COLUMN_NAME_COMMENT_USER_NAME = "username";
	private final static String  COLUMN_NAME_COMMENT_TS = "ts";
	private final static String  COLUMN_NAME_COMMENT_COMMENT_TEXT = "commentText";
	private final static String  COLUMN_NAME_COMMENT_TYPE_KEY = "typeKey";

	// @formatter:off
	private static final String SQL_EVENTS = ""
			+ "select "
				+ "e.id, "
				+ "e.typeId, "
				+ "e.typeRef1, "
				+ "e.typeRef2, "
				+ "e.activeTs, "
				+ "e.rtnApplicable, "
				+ "e.rtnTs, "
				+ "e.rtnCause,   "
				+ "e.alarmLevel, "
				+ "e.message, "
				+ "e.shortMessage, "
				+ "e.ackTs, "
				+ "e.ackUserId, "
				+ "u.username, "
				+ "e.alternateAckSource, "
				+ "ue.silenced "
			+ "from "
				+ "events e   "
				+ "left join users u on e.ackUserId=u.id   "
				+ "left join userEvents ue on e.id=ue.eventId "
			+ "where "
				+ "ue.userId=? and "
				+ "(e.ackTs is null or e.ackTs = 0) "
			+ "order by e.activeTs desc "
			+ "LIMIT 100";


	private static final String SQL_USER_COMMENTS = ""
			+ "select "
			    + "uc.userId, "
			    + "u.username, "
			    + "uc.ts, "
			    + "uc.commentText, "
			    + "uc.typeKey "
			+ "from "
				+ "userComments uc "
				+ "left join users u on uc.userId = u.id "
			+ "where "
				+ "uc.commentType= 1 "
			+ "order by uc.ts";

	// @formatter:on

	private IUserDAO userDAO;

	public PendingEventsDAO() {
		this.userDAO = ApplicationBeans.getUserDaoBean();
	}

	public PendingEventsDAO(IUserDAO userDAO) {
		this.userDAO = userDAO;
	}

	@SuppressWarnings("rawtypes")
	protected  List<UserCommentCache> getUserComents() {
		if (LOG.isTraceEnabled()) {
			LOG.trace("SQL UserComents");
		}

		try {
			@SuppressWarnings("unchecked")
			List<UserCommentCache> listUserComents = DAO.getInstance().getJdbcTemp().query(SQL_USER_COMMENTS,
				(rs, rownumber) -> {
					UserCommentCache user = new UserCommentCache();
					user.setUserId(rs.getInt(COLUMN_NAME_COMMENT_USER_ID));
					user.setUserName(rs.getString(COLUMN_NAME_COMMENT_USER_NAME));
					user.setTs(rs.getLong(COLUMN_NAME_COMMENT_TS));
					user.setCommentText(rs.getString(COLUMN_NAME_COMMENT_COMMENT_TEXT));
					user.setTypeKey(rs.getInt(COLUMN_NAME_COMMENT_TYPE_KEY));
					return user;
				});

			return listUserComents;
		} catch (Exception e) {
			LOG.error(e);
		}
		return null;
	}

	private List<EventInstance> getPendingEvents(int userId, final Map<Integer, List<UserComment>> comments ) {
		if (LOG.isTraceEnabled()) {
			LOG.trace("SQL PendingEvents userId:"+userId);
		}

		try {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			List<EventInstance> listEvents = DAO.getInstance().getJdbcTemp().query(SQL_EVENTS,new Integer[]{userId},
					(rs, rownumber) -> mapToEvent(comments, rs));

			return listEvents;
		} catch (Exception e) {
			LOG.error(e);
		}
		return null;
	}

	private EventInstance mapToEvent(Map<Integer, List<UserComment>> comments, ResultSet rs) throws SQLException {
		int typeId = rs.getInt(COLUMN_NAME_EVENT_TYPE_ID);
		int typeRef1 = rs.getInt(COLUMN_NAME_EVENT_TYPE_REF1);
		int typeRef2 = rs.getInt(COLUMN_NAME_EVENT_TYPE_REF2);
		EventType type = EventTypeUtil.createEventType(typeId,typeRef1,typeRef2);
		long activeTS = rs.getLong(COLUMN_NAME_EVENT_ACTIVE_TS);
		Boolean rtnApplicable = DAO.charToBool(rs.getString(COLUMN_NAME_EVENT_RTN_APPLICABLE));
		int alarmLevel = rs.getInt(COLUMN_NAME_EVENT_ALARM_LEVEL);

		LocalizableMessage message;
		LocalizableMessage shortMessage;
		try {
			message = LocalizableMessage.deserialize(rs.getString(COLUMN_NAME_EVENT_MESSAGE));
			if (rs.getString(COLUMN_NAME_EVENT_SHORT_MESSAGE) == null)
				shortMessage = new LocalizableMessage("common.noMessage");
			else
				shortMessage = LocalizableMessage.deserialize(rs.getString(COLUMN_NAME_EVENT_SHORT_MESSAGE));
		} catch (LocalizableMessageParseException e) {
			message = new LocalizableMessage("common.default",
				rs.getString(COLUMN_NAME_EVENT_MESSAGE));
			shortMessage = new LocalizableMessage("common.default",
					rs.getString(COLUMN_NAME_EVENT_SHORT_MESSAGE));
		}

		EventInstance event = new EventInstance(type, activeTS,	rtnApplicable, alarmLevel, message, shortMessage, null);

		event.setId(rs.getInt(COLUMN_NAME_EVENT_ID));
		long rtnTs = rs.getLong(COLUMN_NAME_EVENT_RTN_TS);
		if (!rs.wasNull())
			event.returnToNormal(rtnTs, rs.getInt(COLUMN_NAME_EVENT_RTN_COUSE));
		
		long ackTs = rs.getLong(COLUMN_NAME_EVENT_ACK_TS);
		
		if (!rs.wasNull()) {
			event.setAcknowledgedTimestamp(ackTs);
			event.setAcknowledgedByUserId(rs.getInt(COLUMN_NAME_EVENT_ACK_USER_ID));
			if (!rs.wasNull()) 
				event.setAcknowledgedByUsername(rs.getString(COLUMN_NAME_EVENT_USERNAME));
			event.setAlternateAckSource(rs.getInt(COLUMN_NAME_EVENT_ALTERNATE_ACK_SOURCE));
		}
		String silent = rs.getString(COLUMN_NAME_EVENT_SILENCED);
		event.setSilenced(DAO.charToBool(silent));
		
		if (!rs.wasNull()) {
			event.setUserNotified(true);
		}

		attachRelationalInfo(event, comments);
		
		return event;
	}


	protected Map<Integer, List<EventInstance>> getPendingEvents() {

		List<Integer> users = userDAO.getAll();

		Map<Integer, List<UserComment>> comments = getCacheUserComments(getUserComents());

		Map<Integer,List<EventInstance>> cacheEvents = new ConcurrentHashMap<>();
		for (int userId: users) {
			List<EventInstance> events = new CopyOnWriteArrayList<>(getPendingEvents(userId, comments));
			cacheEvents.put(userId, events);
		}
		return cacheEvents;
	}

	private void attachRelationalInfo(EventInstance event, Map<Integer, List<UserComment>> comments){
		event.setEventComments(comments.get(event.getId()));
	}

	protected Map<Integer, List<UserComment>> getCacheUserComments(List<UserCommentCache> commentsCache) {

		Map<Integer, List<UserComment>> mappedUserCommentForEvent = new ConcurrentHashMap<>();

		for (UserCommentCache u: commentsCache) {
			int key = u.getTypeKey();
			mappedUserCommentForEvent.putIfAbsent(key, new CopyOnWriteArrayList<>());
			UserComment uc = new UserComment();
			uc.setComment(u.getCommentText());
			uc.setTs(u.getTs());
			uc.setUserId(u.getUserId());
			uc.setUsername(u.getUserName());
			mappedUserCommentForEvent.get(key).add(uc);
		}
		return mappedUserCommentForEvent;
	}
}
