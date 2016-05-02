/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
 * Author: Rijk Ravestein.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.pages.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.print.attribute.standard.MediaSizeName;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.ipp.IppSyntaxException;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.outbox.OutboxInfoDto;
import org.savapage.core.outbox.OutboxInfoDto.OutboxAccountTrxInfoSet;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.print.proxy.JsonProxyPrinter;
import org.savapage.core.print.proxy.ProxyPrintInboxReq;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.OutboxService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.core.util.DateUtil;
import org.savapage.core.util.MediaUtils;
import org.savapage.server.SpSession;
import org.savapage.server.WebApp;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.pages.MessageContent;
import org.savapage.server.webapp.WebAppTypeEnum;

/**
 * A page showing the HOLD proxy print jobs for a user.
 * <p>
 * This page is retrieved from the JavaScript Web App.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public class OutboxAddin extends AbstractUserPage {

    /**
     * .
     */
    private static final long serialVersionUID = 1L;

    /**
     * .
     */
    private static final OutboxService OUTBOX_SERVICE =
            ServiceContext.getServiceFactory().getOutboxService();

    /**
     * .
     */
    private static final ProxyPrintService PROXYPRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /**
     * .
     */
    private static final JobTicketService JOBTICKET_SERVICE =
            ServiceContext.getServiceFactory().getJobTicketService();

    /**
     * .
     */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /**
     * .
     */
    private static final UserDao USER_DAO =
            ServiceContext.getDaoContext().getUserDao();

    /**
     * .
     */
    private class OutboxJobView extends PropertyListView<OutboxJobDto> {

        /**
         * .
         */
        private static final long serialVersionUID = 1L;

        private final boolean isJobticketView;

        /**
         *
         * @param id
         * @param list
         * @param jobticketView
         */
        public OutboxJobView(final String id, final List<OutboxJobDto> list,
                final boolean jobticketView) {
            super(id, list);
            this.isJobticketView = jobticketView;
        }

        @Override
        protected void populateItem(ListItem<OutboxJobDto> item) {

            final MarkupHelper helper = new MarkupHelper(item);
            final OutboxJobDto job = item.getModelObject();
            final boolean isJobTicketItem = job.getUserId() != null;

            Label labelWlk;

            //
            final JsonProxyPrinter cachedPrinter =
                    PROXYPRINT_SERVICE.getCachedPrinter(job.getPrinterName());

            final String printerDisplayName;

            if (cachedPrinter == null) {
                printerDisplayName = "???";
            } else {
                printerDisplayName = StringUtils.defaultString(
                        cachedPrinter.getDbPrinter().getDisplayName(),
                        job.getPrinterName());
            }

            /*
             * Fixed attributes.
             */
            item.add(new Label("printer", printerDisplayName));

            item.add(new Label("timeSubmit",
                    job.getLocaleInfo().getSubmitTime()));

            //
            labelWlk = new Label("timeExpiry",
                    job.getLocaleInfo().getExpiryTime());

            final long now = System.currentTimeMillis();
            final String cssClass;

            if (job.getExpiryTime() < now) {
                cssClass = MarkupHelper.CSS_TXT_ERROR;
            } else if (job.getExpiryTime()
                    - now < DateUtil.DURATION_MSEC_HOUR) {
                cssClass = MarkupHelper.CSS_TXT_WARN;
            } else {
                cssClass = MarkupHelper.CSS_TXT_VALID;
            }

            MarkupHelper.modifyLabelAttr(labelWlk, "class", cssClass);
            item.add(labelWlk);

            //
            final StringBuilder imgSrc = new StringBuilder();

            //
            imgSrc.append(WebApp.PATH_IMAGES).append('/');
            if (isJobTicketItem) {
                imgSrc.append("printer-jobticket-32x32.png");
            } else {
                imgSrc.append("device-card-reader-16x16.png");
            }

            helper.addModifyLabelAttr("img-job", "src", imgSrc.toString());

            //
            helper.encloseLabel("jobticket-remark", job.getComment(),
                    isJobTicketItem
                            && StringUtils.isNotBlank(job.getComment()));

            /*
             * Totals
             */
            StringBuilder totals = new StringBuilder();

            //
            String key = null;
            int total = job.getPages();
            int copies = job.getCopies();

            //
            totals.append(helper.localizedNumber(total));
            key = (total == 1) ? "page" : "pages";
            totals.append(" ").append(localized(key));

            //
            if (copies > 1) {
                totals.append(", ").append(copies).append(" ")
                        .append(localized("copies"));
            }

            //
            total = job.getSheets();
            if (total > 0) {
                key = (total == 1) ? "sheet" : "sheets";
                totals.append(" (").append(total).append(" ")
                        .append(localized(key)).append(")");
            }

            item.add(new Label("totals", totals.toString()));

            //
            final String sparklineData = String.format("%d,%d", job.getSheets(),
                    job.getPages() * job.getCopies() - job.getSheets());
            item.add(new Label("printout-pie", sparklineData));

            //
            final String encloseButtonIdRemove;
            final String encloseButtonIdPreview;

            if (isJobTicketItem) {
                helper.discloseLabel("button-cancel-outbox-job");
                helper.discloseLabel("button-preview-outbox-job");
                encloseButtonIdRemove = "button-cancel-outbox-jobticket";
                encloseButtonIdPreview = "button-preview-outbox-jobticket";
            } else {
                helper.discloseLabel("button-cancel-outbox-jobticket");
                helper.discloseLabel("button-preview-outbox-jobticket");
                encloseButtonIdRemove = "button-cancel-outbox-job";
                encloseButtonIdPreview = "button-preview-outbox-job";
            }

            helper.encloseLabel(encloseButtonIdRemove,
                    getLocalizer().getString("button-cancel", this), true)
                    .add(new AttributeModifier(MarkupHelper.ATTR_DATA_SAVAPAGE,
                            job.getFile()));

            if (job.isDrm()) {
                helper.discloseLabel(encloseButtonIdPreview);
            } else {
                helper.encloseLabel(encloseButtonIdPreview,
                        getLocalizer().getString("button-preview", this), true)
                        .add(new AttributeModifier(
                                MarkupHelper.ATTR_DATA_SAVAPAGE,
                                job.getFile()));
            }
            /*
             * Variable attributes.
             */
            final Map<String, String> mapVisible = new HashMap<>();

            for (final String attr : new String[] { "title", "papersize",
                    "letterhead", "duplex", "singlex", "color", "collate",
                    "grayscale", "accounts", "removeGraphics", "ecoPrint",
                    "extSupplier", "owner-user-name", "drm" }) {
                mapVisible.put(attr, null);
            }

            /*
             *
             */
            mapVisible.put("title", job.getJobName());

            //
            final String mediaOption =
                    ProxyPrintInboxReq.getMediaOption(job.getOptionValues());

            if (mediaOption != null) {
                final MediaSizeName mediaSizeName =
                        MediaUtils.getMediaSizeFromInboxMedia(mediaOption);

                if (mediaSizeName == null) {
                    mapVisible.put("papersize", mediaOption);
                } else {
                    mapVisible.put("papersize",
                            MediaUtils.getUserFriendlyMediaName(mediaSizeName));
                }
            }

            if (ProxyPrintInboxReq.isDuplex(job.getOptionValues())) {
                mapVisible.put("duplex", localized("duplex"));
            } else {
                mapVisible.put("singlex", localized("singlex"));
            }

            if (ProxyPrintInboxReq.isGrayscale(job.getOptionValues())) {
                mapVisible.put("grayscale", localized("grayscale"));
            } else {
                mapVisible.put("color", localized("color"));
            }

            if (job.isCollate() && job.getCopies() > 1 && job.getPages() > 1) {
                mapVisible.put("collate", localized("collate"));
            }

            if (job.isRemoveGraphics()) {
                mapVisible.put("removeGraphics", localized("graphics-removed"));
            }
            if (job.isEcoPrint()) {
                mapVisible.put("ecoPrint", "Eco Print");
            }

            if (job.isDrm()) {
                mapVisible.put("drm", "DRM");
            }

            // Cost
            final StringBuilder cost = new StringBuilder();
            cost.append(job.getLocaleInfo().getCost());

            //
            final OutboxAccountTrxInfoSet trxInfoSet =
                    job.getAccountTransactions();

            if (trxInfoSet != null) {

                final int nAccounts = trxInfoSet.getTransactions().size();

                if (nAccounts > 0) {
                    cost.append(" (").append(nAccounts).append(" ");
                    if (nAccounts == 1) {
                        cost.append(localized("account"));
                    } else {
                        cost.append(localized("accounts"));
                    }
                    cost.append(")");
                }
            }

            item.add(new Label("cost",
                    StringUtils.replace(cost.toString(), " ", "&nbsp;"))
                            .setEscapeModelStrings(false));

            //
            final String extSupplierImgUrl;

            if (job.getExternalSupplierInfo() != null) {
                final ExternalSupplierEnum supplier =
                        job.getExternalSupplierInfo().getSupplier();
                mapVisible.put("extSupplier", supplier.getUiText());
                extSupplierImgUrl = WebApp.getExtSupplierEnumImgUrl(supplier);
            } else {
                extSupplierImgUrl = null;
            }

            if (StringUtils.isBlank(extSupplierImgUrl)) {
                helper.discloseLabel("extSupplierImg");
            } else {
                helper.encloseLabel("extSupplierImg", "", true)
                        .add(new AttributeModifier("src", extSupplierImgUrl));
            }

            //
            if (this.isJobticketView && job.getUserId() != null) {

                helper.encloseLabel("button-jobticket-print",
                        String.format("%s . . .", localized("button-print")),
                        true)
                        .add(new AttributeModifier(
                                MarkupHelper.ATTR_DATA_SAVAPAGE,
                                job.getFile()));

                final org.savapage.core.jpa.User user =
                        USER_DAO.findById(job.getUserId());

                if (user == null) {
                    labelWlk = helper.encloseLabel("owner-user-id",
                            "*** USER NOT FOUND ***", true);
                    MarkupHelper.appendLabelAttr(labelWlk, "class",
                            MarkupHelper.CSS_TXT_WARN);
                } else {
                    helper.encloseLabel("owner-user-id", user.getUserId(),
                            true);
                    mapVisible.put("owner-user-name", user.getFullName());

                    final String email =
                            USER_SERVICE.getPrimaryEmailAddress(user);

                    if (StringUtils.isBlank(email)) {
                        helper.discloseLabel("owner-user-email");
                    } else {
                        labelWlk = helper.encloseLabel("owner-user-email",
                                email, true);
                        MarkupHelper.appendLabelAttr(labelWlk, "href",
                                String.format("mailto:%s", email));
                    }
                }
            } else {
                helper.discloseLabel("owner-user-id");
                helper.discloseLabel("button-jobticket-print");
            }

            /*
             * Hide/Show
             */
            for (Map.Entry<String, String> entry : mapVisible.entrySet()) {

                if (entry.getValue() == null) {
                    entry.setValue("");
                }

                helper.encloseLabel(entry.getKey(), entry.getValue(),
                        StringUtils.isNotBlank(entry.getValue()));
            }
        }
    }

    /**
     * .
     */
    public OutboxAddin(final PageParameters parameters) {

        super(parameters);

        final IRequestParameters parms =
                getRequestCycle().getRequest().getPostParameters();

        final boolean isJobticketView =
                parms.getParameterValue("jobTickets").toBoolean();

        if (isJobticketView
                && getSessionWebAppType() != WebAppTypeEnum.JOBTICKETS
                && getSessionWebAppType() != WebAppTypeEnum.ADMIN) {
            setResponsePage(
                    new MessageContent(AppLogLevelEnum.ERROR, "Access denied"));
            return;
        }

        /*
         * We need the cache to get the alias of the printer later on.
         */
        try {
            PROXYPRINT_SERVICE.lazyInitPrinterCache();
        } catch (IppConnectException | IppSyntaxException e) {
            setResponsePage(
                    new MessageContent(AppLogLevelEnum.ERROR, e.getMessage()));
            return;
        }

        final SpSession session = SpSession.get();

        final OutboxInfoDto outboxInfo;

        if (isJobticketView) {

            outboxInfo = new OutboxInfoDto();

            /*
             * Job Tickets mix-in.
             */
            final Long userKey =
                    parms.getParameterValue("userKey").toOptionalLong();

            final boolean expiryAsc = BooleanUtils.isTrue(
                    parms.getParameterValue("expiryAsc").toOptionalBoolean());

            final List<OutboxJobDto> tickets;

            if (userKey == null) {
                tickets = JOBTICKET_SERVICE.getTickets();
            } else {
                tickets = JOBTICKET_SERVICE.getTickets(userKey);
            }

            Collections.sort(tickets, new Comparator<OutboxJobDto>() {
                @Override
                public int compare(final OutboxJobDto left,
                        final OutboxJobDto right) {
                    final int ret;
                    if (left.getExpiryTime() < right.getExpiryTime()) {
                        ret = -1;
                    } else if (left.getExpiryTime() > right.getExpiryTime()) {
                        ret = 1;
                    } else {
                        ret = 0;
                    }
                    if (expiryAsc) {
                        return ret;
                    }
                    return -ret;
                }
            });

            for (final OutboxJobDto dto : tickets) {
                outboxInfo.addJob(dto.getFile(), dto);
            }

        } else {
            final DaoContext daoContext = ServiceContext.getDaoContext();
            /*
             * Lock user while getting the OutboxInfo.
             */
            daoContext.beginTransaction();

            final org.savapage.core.jpa.User lockedUser =
                    daoContext.getUserDao().lock(session.getUser().getId());

            outboxInfo = OUTBOX_SERVICE.getOutboxJobTicketInfo(lockedUser,
                    ServiceContext.getTransactionDate());

            // unlock
            daoContext.rollback();
        }

        OUTBOX_SERVICE.applyLocaleInfo(outboxInfo, session.getLocale(),
                SpSession.getAppCurrencySymbol());

        final List<OutboxJobDto> entryList = new ArrayList<>();

        for (final Entry<String, OutboxJobDto> entry : outboxInfo.getJobs()
                .entrySet()) {
            entryList.add(entry.getValue());
        }

        //
        add(new OutboxJobView("job-entry", entryList, isJobticketView));
    }

}
