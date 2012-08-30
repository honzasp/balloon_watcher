require 'json'

JSON.parse(STDIN.read).each do |record|
  puts "#{record["time"].to_s},#{record["battery level"]},#{record["battery temperature"]}"
end
