$(function() {
  var data = JSON.parse(decodeURIComponent(location.hash.replace('#', '')));
  if (data.username && data.password) {
    $('#username').val(data.username);
    $('#password').val(data.password);
  }
  $('button').on('click', function() {
    var config = {
      username: $('#username').val(),
      password: $('#password').val()
    };
    location.href = 'pebblejs://close#' + encodeURIComponent(JSON.stringify(config));
    return false;
  });
  $('.honeypot').text(decodeURIComponent(location.hash));
});
