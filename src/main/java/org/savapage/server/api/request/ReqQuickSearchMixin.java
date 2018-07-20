/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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

        private Integer totalResults;

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

        public Integer getTotalResults() {
            return totalResults;
        }

        public void setTotalResults(Integer totalResults) {
            this.totalResults = totalResults;
        }

        /**
         * Calculates and set position and page attributes.
         *
         * @param maxResult
         *            Size of page chunk.
         * @param currPositionCalc
         *            The current position.
         * @param totalResultsCalc
         *            Total number of items.
         */
        public void calcNavPositions(final int maxResult,
                final int currPositionCalc, final int totalResultsCalc) {

            this.currPosition = currPositionCalc;
            this.totalResults = totalResultsCalc;

            if (totalResultsCalc % maxResult == 0) {
                this.lastPage = totalResultsCalc / maxResult;
            } else {
                this.lastPage = totalResultsCalc / maxResult + 1;
            }

            if (currPositionCalc == 0) {
                this.currPage = 1;
            } else {
                this.currPage = 1 + currPositionCalc / maxResult;
            }

            if (currPositionCalc - maxResult > 0) {
                this.prevPosition = currPositionCalc - maxResult;
            } else {
                this.prevPosition = 0;
            }

            if (currPositionCalc + maxResult < totalResultsCalc) {
                this.nextPosition = currPositionCalc + maxResult;
            } else {
                this.nextPosition = currPositionCalc;
            }

            if (totalResultsCalc % maxResult == 0) {
                this.lastPosition = totalResultsCalc - maxResult;
            } else {
                this.lastPosition =
                        totalResultsCalc - totalResultsCalc % maxResult;
            }
        }
    }

}
