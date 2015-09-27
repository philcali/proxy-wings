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

var Integrations = function() {
  var integrations = [ new Pushbullet() ];
  return {
    forEach: function(callback) {
      integrations.forEach(callback);
    }
  };
};
var Integration = function(name) {
  this.name = name;
};
Integration.prototype.getName = function() {
  return this.name;
};
Integration.prototype.login = function(success) {
  $.ajax({
    url: '/' + this.getName() + "/login" + location.search,
    type: 'GET',
    success: success
  });
};
Integration.prototype.authorize = function(baseUrl) {
  return baseUrl + encodeURIComponent(this.getRedirectUrl());
};
Integration.prototype.getRedirectUrl = function() {
  return [
    window.location.protocol + "//",
    location.host,
    "/" + this.getName() + "/auth",
    location.search + "&options=" + location.hash.replace("#", "")
  ].join('');
};
var Pushbullet = function() {
  this.name = 'pushbullet';
};
Pushbullet.prototype = Object.create(Integration.prototype);

var MapView = function(domId) {
  this.zoom = 10;
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
    this.zoom = 10;
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
  this.mapView.setZoom(this.zoom);
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
  , showMap = function(data, zoom) {
      if (!map) {
        $('#fallbackMap').show();
        map = new MapView("fallbackMap");
        map.setMaxResults(data.maxresults || 10, true)
           .setUnits(data.range || 'mi', true)
           .setDistance(data.distance || 10, true);
      }
      if (zoom) {
        map.zoom = zoom;
      } else {
        map.zoom = 10;
      }
      map.redraw(data.fallback);
    };
function initMap() {
  isLoaded = true;
  if (data.fallback) {
    showMap(data);
  } else {
    data.fallback = {
      coords: {
        latitude: 38.9591,
        longitude: -97.9101
      }
    };
    showMap(data, 3);
  }
}

Zepto(function() {
  var longClicks = ['up', 'select', 'down']
    , integrations = new Integrations()
    , queryParams = (function(search) {
        var keyValues = search.replace('?', '')
          , params = {};
        keyValues.split('&').forEach(function(keyValue) {
          var parts = keyValue.split('=');
          params[parts[0]] = '';
          if (parts.length > 1) {
            params[parts[0]] = decodeURIComponent(parts[1]);
          }
        });
        return params;
      })(location.search);
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
  integrations.forEach(function(integration) {
    $('input[name=' + integration.getName() + 'Enabled]').on('change', function() {
      var $container = $('.' + integration.getName());
      if (this.checked) {
        integration.login(function(data, code, xhr) {
          if (xhr.status == 200) {
            $container
              .show()
              .find('.' + integration.getName() + "Authorize")
              .attr('href', integration.authorize(data));
          }
        });
      } else {
        $container.hide();
      }
    });
    if (queryParams[integration.getName()]) {
      $('input[name=' + integration.getName() + 'Enabled]')
        .prop('checked', true)
        .trigger('change');
    }
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
    if (data.integrations) {
      integrations.forEach(function(integration) {
        var checked = data.integrations[integration.getName()] || false;
        $('input[name=' + integration.getName() + 'Enabled]')
          .prop('checked', checked)
          .trigger('changed');
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
    integrations.forEach(function(integration) {
      config.integrations[integration.getName()] = $('input[name=' + integration.getName() + 'Enabled]').prop('checked');
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
