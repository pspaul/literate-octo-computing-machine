/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.ext.papercut;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.UserGroupDao;
import org.savapage.core.dao.UserGroupMemberDao;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserGroupService;
import org.savapage.core.services.UserService;
import org.savapage.core.util.InetUtils;
import org.savapage.server.BasicAuthServlet;

/**
 * PaperCut Custom User Sync Integration as described <a href=
 * "https://www.papercut.com/kb/Main/CaseStudyCustomUserSyncIntegration">here</a>.
 *
 * @author Rijk Ravestein
 *
 */
@WebServlet(name = "ExtPaperCutSyncServlet",
        urlPatterns = { ExtPaperCutSyncServlet.SERVLET_URL_PATTERN })
@ServletSecurity(@HttpConstraint(
        transportGuarantee = TransportGuarantee.CONFIDENTIAL,
        rolesAllowed = { ExtPaperCutSyncServlet.ROLE_ALLOWED }))
public final class ExtPaperCutSyncServlet extends BasicAuthServlet {

    /** */
    private static final long serialVersionUID = 1L;

    /**
     * Role for Basic Authentication.
     */
    public static final String ROLE_ALLOWED = "PaperCutSyncReader";

    /**
     * Base path of custom web files (without leading or trailing '/').
     */
    public static final String PATH_BASE = "ext/papercut";

    /** */
    public static final String SERVLET_URL_PATTERN = "/" + PATH_BASE + "/*";

    /**
     * Path info of User Sync (with leading '/').
     */
    private static final String PATH_INFO_USER_SYNC_PFX = "/user-sync/";

    /** */
    private static final String PATH_INFO_USER_SYNC_IS_VALID =
            PATH_INFO_USER_SYNC_PFX + "is-valid";

    /** */
    private static final String PATH_INFO_USER_SYNC_ALL_USERS =
            PATH_INFO_USER_SYNC_PFX + "all-users";
    /** */
    private static final String PATH_INFO_USER_SYNC_ALL_GROUPS =
            PATH_INFO_USER_SYNC_PFX + "all-groups";

    /** */
    private static final String PATH_INFO_USER_SYNC_GET_USER_DETAILS =
            PATH_INFO_USER_SYNC_PFX + "get-user-details";

    /** */
    private static final String PATH_INFO_USER_SYNC_GROUP_MEMBER_NAMES =
            PATH_INFO_USER_SYNC_PFX + "group-member-names";
    /** */
    private static final String PATH_INFO_USER_SYNC_GROUP_MEMBERS =
            PATH_INFO_USER_SYNC_PFX + "group-members";

    /** */
    private static final String PATH_INFO_USER_SYNC_IS_USER_IN_GROUP =
            PATH_INFO_USER_SYNC_PFX + "is-user-in-group";

    /**
     * Path info of User Auth (with leading '/').
     */
    private static final String PATH_INFO_USER_AUTH = "/user-auth";

    /** */
    private static final String PARM_USERNAME = "username";
    /** */
    private static final String PARM_PASSWORD = "password";

    /** */
    private static final String PARM_GROUPNAME = "groupname";

    /** */
    private static final String RSP_OK = "OK";

    /** */
    private static final String RSP_ERROR = "ERROR";

    /** */
    private static final String RSP_YES = "Y";

    /** */
    private static final String RSP_NO = "N";

    /** */
    private static final int DB_CHUNK_SIZE = 100;

    /** */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /** */
    private static final UserDao USER_DAO =
            ServiceContext.getDaoContext().getUserDao();

    /** */
    private static final UserGroupDao USER_GROUP_DAO =
            ServiceContext.getDaoContext().getUserGroupDao();

    /** */
    private static final UserGroupMemberDao USER_GROUP_MEMBER_DAO =
            ServiceContext.getDaoContext().getUserGroupMemberDao();

    @Override
    public void doGet(final HttpServletRequest request,
            final HttpServletResponse response)
            throws ServletException, IOException {

        if (!ConfigManager.instance()
                .isConfigValue(Key.EXT_PAPERCUT_USER_SYNC_ENABLE)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (!checkBasicAuthAccess(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        response.setContentType("application/txt");

        final String pathInfo =
                StringUtils.defaultString(request.getPathInfo());

        if (pathInfo.equals(PATH_INFO_USER_AUTH)) {

            onUserAuth(request.getParameter(PARM_USERNAME),
                    request.getParameter(PARM_PASSWORD),
                    response.getOutputStream());

        } else if (pathInfo.equals(PATH_INFO_USER_SYNC_IS_VALID)) {

            onUserSyncIsValid(response.getOutputStream());

        } else if (pathInfo.equals(PATH_INFO_USER_SYNC_ALL_USERS)) {

            onUserSyncAllUsers(null, response.getOutputStream());

        } else if (pathInfo.equals(PATH_INFO_USER_SYNC_ALL_GROUPS)) {

            onUserSyncAllGroups(response.getOutputStream());

        } else if (pathInfo.equals(PATH_INFO_USER_SYNC_GET_USER_DETAILS)) {

            if (!onUserSyncGetUserDetails(request.getParameter(PARM_USERNAME),
                    response.getOutputStream())) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }

        } else if (pathInfo.equals(PATH_INFO_USER_SYNC_GROUP_MEMBER_NAMES)
                || pathInfo.equals(PATH_INFO_USER_SYNC_GROUP_MEMBERS)) {

            /*
             * Accept when group is not found (do not set SC_NOT_FOUND).
             */
            onUserSyncGroupMemberNames(request.getParameter(PARM_GROUPNAME),
                    response.getOutputStream());

        } else if (pathInfo.equals(PATH_INFO_USER_SYNC_IS_USER_IN_GROUP)) {

            onUserSyncIsUserInGoup(request.getParameter(PARM_GROUPNAME),
                    request.getParameter(PARM_USERNAME),
                    response.getOutputStream());

        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     *
     * @param username
     *            Username.
     * @param password
     *            Password.
     * @param ostr
     *            Response OutpuStream.
     * @throws IOException
     *             When IO errors.
     */
    private static void onUserAuth(final String username, final String password,
            final OutputStream ostr) throws IOException {

        if (ConfigManager.instance().getUserAuthenticator()
                .authenticate(username, password) != null) {
            ostr.write(RSP_OK.getBytes());
        } else {
            ostr.write(RSP_ERROR.getBytes());
        }
        ostr.write('\n');
    }

    /**
     * @param ostr
     *            Response OutpuStream.
     * @throws IOException
     *             When IO errors.
     */
    private static void onUserSyncIsValid(final OutputStream ostr)
            throws IOException {
        ostr.write(RSP_YES.getBytes());
        ostr.write('\n');
    }

    /**
     * @param userGroupId
     *            If {@code null} all users are selected.
     * @param ostr
     *            Response OutpuStream.
     * @throws IOException
     *             When IO errors.
     */
    private static void onUserSyncAllUsers(final Long userGroupId,
            final OutputStream ostr) throws IOException {

        final UserDao.ListFilter filter = new UserDao.ListFilter();

        filter.setPerson(Boolean.TRUE);
        filter.setDisabled(Boolean.FALSE);
        filter.setDeleted(Boolean.FALSE);

        filter.setUserGroupId(userGroupId);

        int startPosition = 0;
        int listSize = DB_CHUNK_SIZE;

        while (listSize == DB_CHUNK_SIZE) {

            final List<User> list = USER_DAO.getListChunk(filter, startPosition,
                    DB_CHUNK_SIZE, UserDao.Field.USERID, true);

            for (final User user : list) {
                onUserSyncUserDetail(user, ostr);
            }

            listSize = list.size();
            startPosition += DB_CHUNK_SIZE;
        }
    }

    /**
     * @param user
     *            The User.
     * @param ostr
     *            Response OutpuStream.
     * @throws IOException
     *             When IO errors.
     */
    private static void onUserSyncUserDetail(final User user,
            final OutputStream ostr) throws IOException {

        ostr.write(String
                .format("%s\t%s\t%s\t%s\t%s\t%s\n", user.getUserId(),
                        StringUtils.defaultString(user.getFullName()),
                        StringUtils.defaultString(
                                USER_SERVICE.getPrimaryEmailAddress(user)),
                        StringUtils.defaultString(user.getDepartment()),
                        StringUtils.defaultString(user.getOffice()),
                        StringUtils.defaultString(
                                USER_SERVICE.getPrimaryCardNumber(user)))
                .getBytes());
    }

    /**
     * @param ostr
     *            Response OutpuStream.
     * @throws IOException
     *             When IO errors.
     */
    private static void onUserSyncAllGroups(final OutputStream ostr)
            throws IOException {

        final UserGroupService svc =
                ServiceContext.getServiceFactory().getUserGroupService();

        final UserGroupDao.ListFilter filter = new UserGroupDao.ListFilter();

        int startPosition = 0;
        int listSize = DB_CHUNK_SIZE;

        while (listSize == DB_CHUNK_SIZE) {

            final List<UserGroup> list = USER_GROUP_DAO.getListChunk(filter,
                    startPosition, DB_CHUNK_SIZE, UserGroupDao.Field.ID, true);

            for (final UserGroup group : list) {
                if (!svc.isReservedGroupName(group.getGroupName())) {
                    ostr.write(group.getGroupName().getBytes());
                    ostr.write('\n');
                }
            }

            listSize = list.size();
            startPosition += DB_CHUNK_SIZE;
        }
    }

    /**
     * @param groupname
     *            Groupname.
     * @param ostr
     *            Response OutpuStream.
     * @return {@code false when group not found.}
     * @throws IOException
     *             When IO errors.
     */
    private static boolean onUserSyncGroupMemberNames(final String groupname,
            final OutputStream ostr) throws IOException {

        final UserGroup group = USER_GROUP_DAO.findByName(groupname);
        if (group == null) {
            return false;
        }
        onUserSyncAllUsers(group.getId(), ostr);
        return true;
    }

    /**
     * @param groupname
     *            Groupname.
     * @param username
     *            Username.
     * @param ostr
     *            Response OutpuStream.
     * @throws IOException
     *             When IO errors.
     */
    private static void onUserSyncIsUserInGoup(final String groupname,
            final String username, final OutputStream ostr) throws IOException {

        if (USER_GROUP_MEMBER_DAO.isUserInGroup(groupname, username)) {
            ostr.write(RSP_YES.getBytes());
        } else {
            ostr.write(RSP_NO.getBytes());
        }
        ostr.write('\n');
    }

    /**
     * @param username
     *            Username.
     * @param ostr
     *            Response OutpuStream.
     * @return {@code false} when user is not found.
     * @throws IOException
     *             When IO errors.
     */
    private static boolean onUserSyncGetUserDetails(final String username,
            final OutputStream ostr) throws IOException {

        final User user = USER_DAO.findActiveUserByUserId(username);
        if (user == null) {
            return false;
        }
        onUserSyncUserDetail(user, ostr);
        return true;
    }

    @Override
    protected boolean isBasicAuthValid(final String username, final String pw) {

        final ConfigManager cm = ConfigManager.instance();

        return StringUtils.isNotBlank(username) && StringUtils.isNotBlank(pw)
                && cm.getConfigValue(Key.EXT_PAPERCUT_USER_SYNC_USERNAME)
                        .equals(username)
                && cm.getConfigValue(Key.EXT_PAPERCUT_USER_SYNC_PASSWORD)
                        .equals(pw);
    }

    @Override
    protected boolean isRemoteAddrAllowed(final String remoteAddr) {
        final String cidrRanges = ConfigManager.instance().getConfigValue(
                Key.EXT_PAPERCUT_USER_SYNC_IP_ADDRESSES_ALLOWED);

        return !StringUtils.isBlank(cidrRanges)
                && InetUtils.isIpAddrInCidrRanges(cidrRanges, remoteAddr);
    }

}
