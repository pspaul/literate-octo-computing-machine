/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.server.api.request;

import org.savapage.core.dto.AbstractDto;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class ReqQuickSearchMixin extends ApiRequestMixin {

    /**
     *
     * @author Rijk Ravestein
     *
     */
    protected static class DtoQuickSearchRsp extends AbstractDto {

        private Integer nextPosition;
        private Integer prevPosition;
        private Integer lastPosition;
        private Integer currPosition;

        private Integer currPage;
        private Integer lastPage;

        public Integer getNextPosition() {
            return nextPosition;
        }

        public void setNextPosition(Integer nextPosition) {
            this.nextPosition = nextPosition;
        }

        public Integer getPrevPosition() {
            return prevPosition;
        }

        public void setPrevPosition(Integer prevPosition) {
            this.prevPosition = prevPosition;
        }

        public Integer getLastPosition() {
            return lastPosition;
        }

        public void setLastPosition(Integer lastPosition) {
            this.lastPosition = lastPosition;
        }

        public Integer getCurrPosition() {
            return currPosition;
        }

        public void setCurrPosition(Integer currPosition) {
            this.currPosition = currPosition;
        }

        public Integer getCurrPage() {
            return currPage;
        }

        public void setCurrPage(Integer currPage) {
            this.currPage = currPage;
        }

        public Integer getLastPage() {
            return lastPage;
        }

        public void setLastPage(Integer lastPage) {
            this.lastPage = lastPage;
        }

        /**
         * Calculates and set position and page attributes.
         *
         * @param maxResult
         *            Size of page chunk.
         * @param currPositionCalc
         *            The current position.
         * @param nTotalItems
         *            Total number of items.
         */
        public void calcNavPositions(final int maxResult,
                final int currPositionCalc, final int nTotalItems) {

            final int lastPageCalc;
            if (nTotalItems % maxResult == 0) {
                lastPageCalc = nTotalItems / maxResult;
            } else {
                lastPageCalc = nTotalItems / maxResult + 1;
            }
            this.setLastPage(lastPageCalc);

            if (currPositionCalc == 0) {
                this.setCurrPage(1);
            } else {
                this.setCurrPage(1 + currPositionCalc / maxResult);
            }

            if (currPositionCalc == 0) {
                this.setCurrPage(1);
            } else {
                this.setCurrPage(1 + currPositionCalc / maxResult);
            }

            this.setCurrPosition(currPositionCalc);

            final int prevPositionCalc;
            if (currPositionCalc - maxResult > 0) {
                prevPositionCalc = currPositionCalc - maxResult;
            } else {
                prevPositionCalc = 0;
            }
            this.setPrevPosition(prevPositionCalc);

            final int nextPositionCalc;
            if (currPositionCalc + maxResult < nTotalItems) {
                nextPositionCalc = currPositionCalc + maxResult;
            } else {
                nextPositionCalc = currPositionCalc;
            }

            final int lastPositionCalc;
            if (nTotalItems % maxResult == 0) {
                lastPositionCalc = nTotalItems - maxResult;
            } else {
                lastPositionCalc = nTotalItems - nTotalItems % maxResult;
            }

            this.setNextPosition(nextPositionCalc);
            this.setLastPosition(lastPositionCalc);
        }

    }

}
