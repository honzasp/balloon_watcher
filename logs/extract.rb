require 'json'

puts STDIN.map { |line|
  info = {}

  line.match /^(\d+)-(\d+)-(\d+) (\d+):(\d+):(\d+)/ do |md|
    info["time"] = Time.new *(md.to_a[1..-1].map &:to_i)
  end

  line.scan /([A-Z][a-zA-Z]*) ([0-9a-z.-]+)/ do |header, value|
    case header
    when "Lat"
      info["latitude"] = value.to_f
    when "Lng"
      info["longitude"] = value.to_f
    when "Alt"
      info["altitude"] = value.to_f
    when "Acc"
      info["accuracy"] = value.to_f
    when "GSM"
      info["gsm signal"] = value.to_i
    when "BtL"
      info["battery level"] = value.to_f
    when "BtH"
      info["battery health"] = value.to_s
    when "BtT"
      info["battery temperature"] = value.to_i
    end
  end

  info
}.to_json
