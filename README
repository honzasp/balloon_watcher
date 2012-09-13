# Balloon watcher

Watch your balloon with balloon watcher! :)

## Log format

### Long log

#### Location

 * Lat <latitude>
 * Lng <longitude>
 * Alt <altitude> (*)
 * Acc <accuracy> (*)
 * Brg <bearing> (*)
 * Spd <speed> (*)

#### Signal

 * GSM <gsm signal strength>
 * Signal unknown

GSM signal strength is a number from 0 to 31 (the bigger the better).

#### Battery

 * BtL <battery level>
 * BtH <battery health>
 * BtT <battery temperature>
 * Battery unknown

### Short log

#### Time

 * <hour>:<minute>

#### Location

 * T<latitude>
 * G<longitude>
 * A<altitude> (*)
 * C<accuracy> (*)
 * S<speed> (*)
 * Loc? (unknown location)

#### Signal

 * M<gsm signal>

#### Battery

 * B<battery level><battery health>

## SMS instructions

The message must begin with `bw `. After this header can be one or more
instructions:

 * `sendlog` - immediatly reply with a short log
 * `restart` - restarts the watching
 * `photo` - take a photo
