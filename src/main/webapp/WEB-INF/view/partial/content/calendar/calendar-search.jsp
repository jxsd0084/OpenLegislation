<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="open-component" tagdir="/WEB-INF/tags/component" %>

<section ng-controller="CalendarSearchPageCtrl" ng-init="setHeaderVisible(true)">
  <md-tabs class="md-primary" md-selected="activeIndex">

    <md-tab md-on-select="setCalendarHeaderText()">
      <md-tab-label>
        <i class="icon-search prefix-icon2"></i>Search
      </md-tab-label>
      <section ng-controller="CalendarSearchCtrl">
        <form name="calendar-search-form">
          <md-content class="padding-20">
            <md-input-container class="md-primary">
              <label><i class="prefix-icon2 icon-search"></i>Search for calendars</label>
              <input tabindex="1" style="font-size:1.4rem;" name="quick-term"
                  ng-model="searchTerm" ng-model-options="{debounce: 300}" ng-change="termSearch(true)">
            </md-input-container>
          </md-content>
          <md-divider></md-divider>
          <md-subheader ng-show="searched && searchTerm && pagination.totalItems === 0"
              class="margin-10 md-warn md-whiteframe-z0">
            <h4>No search results were found for '{{searchTerm}}'</h4>
          </md-subheader>
        </form>
        <section ng-show="searched && pagination.totalItems > 0">
          <md-card class="content-card">
            <md-subheader>
              {{pagination.totalItems}} calendars were matched.&nbsp;&nbsp;
              Viewing page {{pagination.currPage}} of {{pagination.lastPage}}.
            </md-subheader>
            <md-content class="no-top-margin">
              <md-list>
                <a ng-repeat="r in searchResults" ng-init="cal = r.result; highlights = r.highlights;"
                    class="result-link" ng-href="{{getCalendarUrl(cal.year, cal.calendarNumber)}}"
                    ng-click="changeTab(cal.activeLists.size>0 ? 'active-list' : 'floor')">
                  <md-item>
                    <md-item-content layout-sm="column" layout-align-sm="center start"
                        style="cursor: pointer;" >
                      <div layout-sm="row" layout-align-sm="start end" style="width:180px;padding:16px;">
                        <h3 class="no-margin">
                          {{cal.year}} &#35;{{cal.calendarNumber}}
                          <span hide-gt-sm>&nbsp;</span>
                        </h3>
                        <h5 class="no-margin">
                          {{cal.calDate | moment:'MMMM D'}}
                        </h5>
                      </div>
                      <div class="md-tile-content" layout="column">
                        <div layout-gt-sm="row" layout-align="start end">
                          <h5 class="no-margin" style="width: 200px">
                            {{cal.activeLists.size}} Active List Supplementals
                          </h5>
                          <h6 class="no-margin">
                            {{getTotalActiveListBills(cal)}} Total Active List Bills
                          </h6>
                        </div>
                        <div layout-gt-sm="row" layout-align="start end">
                          <h5 class="no-margin" style="width: 200px">
                            {{(cal.floorCalendar.year ? 1 : 0) + cal.supplementalCalendars.size}}
                            Floor Supplementals
                          </h5>
                          <h6 class="no-margin">
                            {{getTotalFloorBills(cal)}} Total Floor Bills
                          </h6>
                        </div>
                      </div>
                    </md-item-content>
                    <%--<md-divider ng-if="!$last"/>--%>
                  </md-item>
                  <md-divider hide-gt-sm ng-hide="$last"></md-divider>
                </a>
              </md-list>
            </md-content>
            <div ng-show="pagination.needsPagination()" class="text-medium margin-10 padding-10"
                layout="row" layout-align="left center">
              <md-button ng-click="paginate('first')" class="md-primary md-no-ink margin-right-10">
                <i class="icon-first"></i>&nbsp;First
              </md-button>
              <md-button ng-disabled="!pagination.hasPrevPage()"
                  ng-click="paginate('prev')" class="md-primary md-no-ink margin-right-10">
                <i class="icon-arrow-left5"></i>&nbsp;Previous
              </md-button>
              <md-button ng-click="paginate('next')"
                  ng-disabled="!pagination.hasNextPage()"
                  class="md-primary md-no-ink margin-right-10">
                Next&nbsp;<i class="icon-arrow-right5"></i>
              </md-button>
              <md-button ng-click="paginate('last')" class="md-primary md-no-ink margin-right-10">
                Last&nbsp;<i class="icon-last"></i>
              </md-button>
            </div>
          </md-card>
        </section>
      </section>
    </md-tab>

    <!-- Calendar Date Picker -->
    <md-tab md-on-select="setCalendarHeaderText()">
      <md-tab-label>
        <i class="icon-calendar prefix-icon2"></i>Browse
      </md-tab-label>
      <section ng-controller="CalendarPickCtrl">
        <div id="calendar-date-picker" ui-calendar="calendarConfig" ng-model="eventSources"></div>
      </section>
    </md-tab>

    <!-- Calendar Updates -->
    <md-tab label="Updates" md-on-select="setCalendarHeaderText()">
      <section ng-init="">

      </section>
    </md-tab>
  </md-tabs>
</section>
