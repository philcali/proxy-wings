Zepto(function() {
  var data = JSON.parse(decodeURIComponent(location.hash.replace('#', '')))
    , longClicks = ['up', 'select', 'down'];
  if (data.username && data.password) {
    $('#username').val(data.username);
    $('#password').val(data.password);
    $('input[value=' + data.region + ']').prop('checked', true);
    $('input[value=' + data.range + ']').prop('checked', true);
    if (data.longClicks) {
      longClicks.forEach(function(button) {
        $('select[name=' + button + 'LongClick]').val(data.longClicks[button]);
      });
    }
  } else {
    $('input[value=us]').prop('checked', true);
    $('input[value=mi]').prop('checked', true);
  }
  $('input[type=button]').on('click', function() {
    var config = {
      username: $('#username').val(),
      password: $('#password').val(),
      range: $('[name=range]:checked').val(),
      region: $('[name=region]:checked').val(),
      longClicks: {}
    };
    longClicks.forEach(function(button) {
      config.longClicks[button] = $('select[name=' + button + 'LongClick]').val();
    });
    location.href = 'pebblejs://close#' + encodeURIComponent(JSON.stringify(config));
    return false;
  });
  $('.honeypot').text(decodeURIComponent(location.hash));
});
