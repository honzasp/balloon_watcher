# Balloon watcher

Watch your balloon with balloon watcher! :)

## Log format

### Long log

Long logs are saved to the log file.

#### Location

 * `Lat <latitude>`
 * `Lng <longitude>`
 * `Alt <altitude>` (when known)
 * `Acc <accuracy>` (when known)
 * `Brg <bearing>` (when known)
 * `Spd <speed>` (when known)

#### Signal

 * `GSM <gsm signal strength>`
 * `Signal unknown`

GSM signal strength is a number from 0 to 31 (the bigger the better).

#### Battery

 * `BtL <battery level>`
 * `BtH <battery health>`
 * `BtT <battery temperature>`
 * `Battery unknown`

### Short log

Short logs are send via SMS messages.

#### Time

 * `<hour>:<minute>`

#### Location

 * `T<latitude>`
 * `G<longitude>`
 * `A<altitude>` (when known)
 * `C<accuracy>` (when known)
 * `S<speed>` (when known)
 * `Loc?` (when the location is unknown)

#### Signal

 * `M<gsm signal>`

#### Battery

 * `B<battery level><battery health>`

## SMS instructions

The message must begin with `bw `. After this header can be one or more
instructions:

 * `sendlog` - immediatly reply with a short log
 * `restart` - restarts the watching
 * `photo` - take a photo
