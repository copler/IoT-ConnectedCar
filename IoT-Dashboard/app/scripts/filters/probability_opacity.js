'use strict';

angular.module('iotDashboard')
  .filter('probabilityOpacity', function(){
    return function probabilityOpacity(probability){
      return (0.34 + 0.66 * probability);
    };
  });

angular.module('iotDashboard')
.filter('journeyClass', function(){
  return function journeyClass(journey){
    return journey.destination ? 'r' : 'b';
  };
});