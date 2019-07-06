#
# This file is part of the SavaPage project <https://www.savapage.org>.
# Copyright (c) 2011-2019 Datraverse B.V.
# Author: Rijk Ravestein.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#
# For more information, please contact Datraverse B.V. at this
# address: info@datraverse.com
#

.PHONY: help
help:
	@echo Please enter a target.

.PHONY: clean
clean:
	mvn clean

.PHONY: package
package:
	mvn package

.PHONY: repackage
repackage: clean package wadl javadoc

.PHONY: wadl
wadl:
	mvn com.sun.jersey.contribs:maven-wadl-plugin:generate

.PHONY: javadoc
javadoc:
	mvn javadoc:javadoc

# end-of-file
