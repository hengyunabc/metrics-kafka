# example of converting dropwizard metrics json into wavefront data for proxy.
# supporting point tags in the message payload
# howard yoo 2017.12.4
import json

# example json string to parse.
json_string = """
{
    "timers": {
        "test.test-timer": {
            "count": 43,
            "max": 505.33599999999996,
            "mean": 502.585391215306,
            "min": 500.191,
            "p50": 502.443,
            "p75": 504.046,
            "p95": 505.291,
            "p98": 505.33599999999996,
            "p99": 505.33599999999996,
            "p999": 505.33599999999996,
            "stddev": 1.6838970975560197,
            "m15_rate": 0.8076284847453551,
            "m1_rate": 0.8883929708459906,
            "m5_rate": 0.8220236458023953,
            "mean_rate": 0.9799289583409866,
            "duration_units": "milliseconds",
            "rate_units": "calls/second"
        }
    },
    "durationUnit": "milliseconds",
    "meters": {},
    "clock": 1453287302764,
    "hostName": "localhost",
    "rateUnit": "second",
    "histograms": {
        "test.response-sizes": {
            "count": 43,
            "max": 142,
            "mean": 123.29413148075862,
            "min": 100,
            "p50": 124,
            "p75": 134,
            "p95": 141,
            "p98": 142,
            "p99": 142,
            "p999": 142,
            "stddev": 12.28197980813012
        }
    },
    "counters": {},
    "gauges": {
        "test.jvm.mem.pools.Code-Cache.used": {
            "value": 769088
        },
        "test.jvm.mem.pools.Code-Cache.usage": {
            "value": 0.015280405680338541
        },
        "test.jvm.mem.heap.committed": {
            "value": 128974848
        },
        "test.jvm.mem.pools.PS-Old-Gen.usage": {
            "value": 0.00048653738839285715
        },
        "test.jvm.mem.non-heap.used": {
            "value": 17222048
        },
        "test.jvm.gc.PS-MarkSweep.count": {
            "value": 0
        },
        "test.jvm.mem.pools.Code-Cache.init": {
            "value": 2555904
        },
        "test.jvm.mem.pools.PS-Survivor-Space.usage": {
            "value": 0.99683837890625
        },
        "test.jvm.mem.pools.PS-Eden-Space.max": {
            "value": 705691648
        },
        "test.jvm.mem.pools.PS-Perm-Gen.init": {
            "value": 22020096
        },
        "test.jvm.mem.total.init": {
            "value": 158793728
        },
        "test.jvm.mem.heap.max": {
            "value": 1908932608
        },
        "test.jvm.mem.heap.init": {
            "value": 134217728
        },
        "test.jvm.mem.pools.PS-Eden-Space.usage": {
            "value": 0.039622597318878856
        },
        "test.jvm.mem.pools.PS-Survivor-Space.used": {
            "value": 5226304
        },
        "test.jvm.mem.pools.Code-Cache.committed": {
            "value": 2555904
        },
        "test.jvm.mem.pools.PS-Old-Gen.committed": {
            "value": 89128960
        },
        "test.jvm.mem.non-heap.max": {
            "value": 136314880
        },
        "test.jvm.gc.PS-Scavenge.count": {
            "value": 1
        },
        "test.jvm.mem.pools.PS-Survivor-Space.init": {
            "value": 5242880
        },
        "test.jvm.mem.pools.PS-Perm-Gen.committed": {
            "value": 22020096
        },
        "test.jvm.mem.pools.PS-Eden-Space.used": {
            "value": 27961336
        },
        "test.jvm.mem.pools.PS-Old-Gen.used": {
            "value": 696384
        },
        "test.jvm.mem.pools.Code-Cache.max": {
            "value": 50331648
        },
        "test.jvm.mem.pools.PS-Perm-Gen.usage": {
            "value": 0.19135079732755336
        },
        "test.jvm.mem.total.committed": {
            "value": 153550848
        },
        "test.jvm.mem.non-heap.init": {
            "value": 24576000
        },
        "test.jvm.mem.pools.PS-Eden-Space.committed": {
            "value": 34603008
        },
        "test.jvm.mem.total.max": {
            "value": 2045247488
        },
        "test.jvm.mem.pools.PS-Survivor-Space.committed": {
            "value": 5242880
        },
        "test.jvm.gc.PS-MarkSweep.time": {
            "value": 0
        },
        "test.jvm.mem.heap.used": {
            "value": 33884024
        },
        "test.jvm.mem.heap.usage": {
            "value": 0.017750246319853318
        },
        "test.jvm.mem.pools.PS-Perm-Gen.max": {
            "value": 85983232
        },
        "test.jvm.mem.pools.PS-Survivor-Space.max": {
            "value": 5242880
        },
        "test.jvm.mem.pools.PS-Old-Gen.init": {
            "value": 89128960
        },
        "test.jvm.mem.total.used": {
            "value": 51106240
        },
        "test.jvm.mem.pools.PS-Perm-Gen.used": {
            "value": 16453128
        },
        "test.jvm.mem.pools.PS-Eden-Space.init": {
            "value": 34603008
        },
        "test.jvm.mem.non-heap.committed": {
            "value": 24576000
        },
        "test.jvm.gc.PS-Scavenge.time": {
            "value": 19
        },
        "test.jvm.mem.pools.PS-Old-Gen.max": {
            "value": 1431306240
        },
        "test.jvm.mem.non-heap.usage": {
            "value": 0.12634142362154446
        }
    },
    "ip": "192.158.1.113",
    "pointTags":{  
      "appName":"myapp",
      "cluster":"c-1"
   }
}
"""

def merge_two_dicts(x, y):
	z = x.copy()
	z.update(y)
	return z

def convertTimer(a_timer):
	if a_timer is None:
		return ""
	
	m_map = {}
	l_pointTag = {}
	s = ""

	for key in a_timer:
		values = a_timer[key]
		for value in values:
			v = values[value];
			if type(v).__name__ == "str" or type(v).__name__ == "unicode":
				l_pointTag[value] = v
			else:
				m_map[key + "." + value] = values[value];

	m_pointTag = merge_two_dicts(g_pointTag, l_pointTag)

	# now, print out all the metrics along with merged local and global pointtags
	for mname in m_map:
		s += toWavefrontData(mname, str(m_map[mname]), source, str(clock), m_pointTag) + "\n"

	return s

def convertGauge(a_gauge):
	if a_gauge is None:
		return ""

	s = ""
	for key in a_gauge:
		s += toWavefrontData(key, str(a_gauge[key]["value"]), source, str(clock), g_pointTag) + "\n"
	return s

def convertCounter(a_counter):
	if a_counter is None:
		return ""

	s = ""
	for key in a_counter:
		s += toWavefrontData(key, str(a_counter[key]["count"]), source, str(clock), g_pointTag) + "\n"
	return s

def convertHistogram(a_histogram):
	if a_histogram is None:
		return ""

	s = ""
	for key in a_histogram:
		values = a_histogram[key]
		for value in values:
			s += toWavefrontData(key + "." + value, str(values[value]), source, str(clock), g_pointTag) + "\n"
	return s


def pointTagsToStr(pointTags):
	s = ""
	for key in pointTags:
		s += " "
		s += key
		s += "="
		s += "\"" + pointTags[key] + "\""
	return s

def toWavefrontData(metricName, value, source, timestamp, pointTags):
	s = ""
	if metricName != "":
		if value != "":
			if source != "":
				s += metricName + " " + value
				if timestamp != "":
					s += " " + timestamp
				s += " source=\"" + source + "\""
				if pointTags is not None:
					s += pointTagsToStr(pointTags)
	return s

def convertMetricsToWavefront(jsondata):
	s = ""
	s += convertTimer(jsondata['timers'])
	s += convertHistogram(jsondata['histograms'])
	s += convertCounter(jsondata['counters'])
	s += convertGauge(jsondata['gauges'])
	return s

# --------------- MAIN CODE ------------------

# parse the json into json object
jsondata = json.loads(json_string)

# collect the following global parameters
ip = jsondata['ip']
hostname = jsondata['hostName']
clock = jsondata['clock']
rateUnit = jsondata['rateUnit']
durationUnit = jsondata['durationUnit']

# define source. if localhost is the hostname, use ip instead
source = hostname
if source == "localhost":
	source = ip

# global point tag
g_pointTag = {}

# if point tags exist, use it.
if 'pointTags' in jsondata:
    g_pointTag = jsondata['pointTags']

# append additional point tags
g_pointTag["rateUnit"] = str(rateUnit)
g_pointTag["durationUnit"] = str(durationUnit)

# main convert function
print(convertMetricsToWavefront(jsondata))

print "--- end ---"

# end

