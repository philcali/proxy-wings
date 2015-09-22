var OCMViewer = function(config) {
  this.baseUrl = 'http://api.openchargemap.io/v2/poi/';
  this.config = config || {
    output: 'json',
    distance: 10,
    maxresults: 50,
    verbose: false,
    camelcase: true,
    distanceunit: 'Miles'
  };
};
OCMViewer.prototype.getStations = function(coords, callback) {
  this.config.latitude = coords.lat;
  this.config.longitude = coords.lng;
  $.getJSON(this.baseUrl, this.config, function(stations) {
    stations.forEach(callback);
  });
};

var MapView = function(domId) {
  this.ocm = new OCMViewer();
  this.position = {
    lat: 0,
    lng: 0
  };
  this.config = {
    range: 'mi',
    distance: 10,
    meters: function() {
      return this.range == 'mi' ?
        this.distance * 1609.34 :
        this.distance * 1000;
    },
    units: function() {
      return this.range == 'mi' ? 'Miles' : 'KM';
    }
  };
  this.info = new google.maps.InfoWindow();
  this.mapView = new google.maps.Map(document.getElementById(domId), {
    center: this.position,
    streetViewControl: false,
    rotateControl: false,
    panControl: false,
    disableDefaultUI: true,
    disableDoubleClickZoom: true,
    overviewMapControl: false,
  });
  this.selfCircle = new google.maps.Circle({
    radius: this.config.meters(),
    map: this.mapView,
    center: this.position,
    clickable: false,
    draggable: true,
    strokeColor: '#005CE6',
    strokeWeight: 2,
    strokeOpacity: 0.8,
    fillColor: '#005CE6',
    fillOpacity: 0.2,
    zIndex: 1
  });
  this.markers = [];
  this.mapView.addListener('dblclick', (function(e) {
    this.position = {
      lat: e.latLng.H,
      lng: e.latLng.L
    };
    this.redraw();
  }).bind(this));
};

MapView.prototype.clearMap = function() {
  this.markers.forEach(function(marker) {
    marker.setMap(null);
  });
  this.selfCircle.setMap(null);
};

MapView.prototype.setMaxResults = function(maxresults, silent) {
  this.ocm.config.maxresults = maxresults;
  if (!silent) {
    this.redraw();
  }
  return this;
};

MapView.prototype.setDistance = function(distance, silent) {
  this.ocm.config.distance = distance;
  this.config.distance = distance;
  if (!silent) {
    this.redraw();
  }
  return this;
};

MapView.prototype.setUnits = function(units, silent) {
  this.config.range = units;
  this.ocm.config.distanceunit = this.config.units();
  if (!silent) {
    this.redraw();
  }
  return this;
};

MapView.prototype.redraw = function(position) {
  if (position) {
    this.position = {
      lat: position.coords.latitude,
      lng: position.coords.longitude
    };
  }
  this.clearMap();
  this.markers = [];
  this.mapView.setZoom(10);
  this.mapView.setCenter(this.position);
  this.selfCircle.setMap(this.mapView);
  this.selfCircle.setRadius(this.config.meters());
  this.selfCircle.setCenter(this.position);
  this.ocm.getStations(this.position, this.addStation.bind(this));
  return this;
};

MapView.prototype.addStation = function(station) {
  var marker = new google.maps.Marker({
    map: this.mapView,
    position: {
      lat: station.addressInfo.latitude,
      lng: station.addressInfo.longitude
    }
  });
  this.markers.push(marker);
  marker.addListener('click', this.displayInfo(marker, station));
};

MapView.prototype.displayInfo = function(marker, station) {
  return (function() {
    if (station) {
      this.info.setContent([
        station.addressInfo.title,
        '-',
        station.addressInfo.addressLine1
      ].join(' '));
    }
    this.info.open(this.mapView, marker);
  }).bind(this);
};

var map = null
  , data = JSON.parse(decodeURIComponent(location.hash.replace('#', '')))
  , isLoaded = false
  , navConfig = {
      enableHighAccuracy: true,
      timeout: 10000,
      maximumAge: 10000
    }
  , showMap = function(data) {
      if (!map) {
        $('#fallbackMap').show();
        map = new MapView("fallbackMap");
        map.setMaxResults(data.maxresults || 10, true)
           .setUnits(data.range || 'mi', true)
           .setDistance(data.distance || 10, true);
      }
      map.redraw(data.fallback);
    };
function initMap() {
  isLoaded = true;
  if (data.fallback) {
    showMap(data);
  }
  var load = function(position) {
        $('.fallbackButton').show();
        data.fallback = position;
        showMap(data);
      }
    , log = function(error) {
        var userFacing = "";
        switch (error.code) {
          case 2:
            userFacing = "Unable to pull your current position.";
            break;
          case 3:
            userFacing = "Location timed out.";
        }
        if (userFacing != "") {
          userFacing += " Please try again.";
          $('.fallbackError').text(userFacing).show();
        }
        console.log("ERROR(" + error.code + "): " + error.message);
      };
  $('.fallbackButton > .item-button').on('click', function() {
    $('.fallbackError').hide();
    navigator.geolocation.getCurrentPosition(load, log, navConfig);
  });
  navigator.geolocation.getCurrentPosition(load, log, navConfig);
}

Zepto(function() {
  var longClicks = ['up', 'select', 'down'];
  $('[name=distance]').on('change', function(e) {
    map.setDistance(this.value);
  });
  $('[name=range]').on('change', function(e) {
    if (this.checked) {
      map.setUnits(this.value);
    }
  });
  $('[name=maxresults]').on('change', function(e) {
    map.setMaxResults(this.value);
  });
  if (data.username && data.password) {
    $('#username').val(data.username);
    $('#password').val(data.password);
    $('input[value=' + data.region + ']').prop('checked', true);
    $('input[value=' + data.range + ']').prop('checked', true);
    if (data.distance) {
      $('input[name=distance]').val(data.distance);
    }
    if (data.maxresults) {
      $('input[name=maxresults]').val(data.maxresults);
    }
    if (data.fallback) {
      $('input[name=latitude]').val(data.fallback.coords.latitude);
      $('input[name=longitude]').val(data.fallback.coords.longitude);
    }
    if (data.longClicks) {
      longClicks.forEach(function(button) {
        $('select[name=' + button + 'LongClick]').val(data.longClicks[button]);
      });
    }
  } else {
    $('input[value=us]').prop('checked', true);
    $('input[value=mi]').prop('checked', true);
  }
  $('.submit').on('click', function() {
    var config = {
      username: $('#username').val(),
      password: $('#password').val(),
      range: $('[name=range]:checked').val(),
      region: $('[name=region]:checked').val(),
      longClicks: {},
      distance: parseInt($('.item-input[name=distance]').val()),
      maxresults: parseInt($('.item-input[name=maxresults]').val())
    };
    longClicks.forEach(function(button) {
      config.longClicks[button] = $('select[name=' + button + 'LongClick]').val();
    });
    if (map) {
      var center = map.selfCircle.getCenter();
      config.fallback = {
        coords: {
          latitude: center.H,
          longitude: center.L
        }
      };
    }

    location.href = 'pebblejs://close#' + encodeURIComponent(JSON.stringify(config));
    return false;
  });
  $('.honeypot').text(decodeURIComponent(location.hash));
});
