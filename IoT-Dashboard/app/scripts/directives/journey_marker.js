'use strict';

angular.module('iotDashboard')
  .directive('journeyMarker', function() {
    return {
      restrict: 'E',
      scope: {
        journey: '=',
        isCurrentJourney: '=',
        setCurrentJourneyCallback: '='
      },
      link: function(scope) {
        scope.clickMarkerCallback = function clickMarkerCallback() {
          scope.setCurrentJourneyCallback(scope.journey);
        };

        scope.closeWindowCallback = function closeWindowCallback() {
          scope.setCurrentJourneyCallback();
        };

        scope.journeyMarkerIcon = {
          url: 'images/icon_marker.png',
          scaledSize: { height: 40, width: 30 }
        };

        scope.journeyMarkerIconR = {
          url: 'images/icon_marker_r.png',
          scaledSize: { height: 40, width: 30 }
        };

        scope.journeyMarkerIconG = {
          url: 'images/icon_marker_g.png',
          scaledSize: { height: 40, width: 30 }
        };

        scope.journeyMarkerIconB = {
          url: 'images/icon_marker_b.png',
          scaledSize: { height: 40, width: 30 }
        };

        scope.id = scope.journey.id.toString();
      },
      templateUrl: 'templates/journey_marker.html'
    };
  });
