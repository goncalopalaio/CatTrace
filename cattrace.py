import sys
import json


TAG = "CatTrace"

EVENT_TYPE_SYNC = "clock_sync"
EVENT_TYPE_BEGIN = "B"
EVENT_TYPE_END = "E"
EVENT_TYPE_COMPLETE = "X"
EVENT_TYPE_INSTANT = "i"
EVENT_TYPE_METADATA = "M"

GLOBAL_FILE = "global"

TRACES = {}

def parse(text):
	try:
		return None, json.loads(text)
	except Exception as e:
		return e, None

def main():
	print("main")
	file = "default"

	while True:
		line = sys.stdin.readline()
		line = line.strip()

		if not line:
			break
		
		start = line.find(TAG)
		if start < 0:
			print(f"Failed to find tag | {line=}")
			continue

		start = len(TAG) + start + 1

		error, event = parse(line[start:])
		
		if error:
			print(f"{error=}, {line=}")
			continue

		event_type = event["ph"]
		pid = event["pid"]
		name = event["name"]

		if event_type == "M" and name == "process_name":
			process_name = event["args"]["name"]
			args = event["args"]

			# A new file will be created
			if "type" in args and args["type"] == "START":
				file = f"{process_name}-{pid}"

		if file not in TRACES:
			TRACES[file] = []
			print(f"\n\nNew file | {file=}\n\n")

		if GLOBAL_FILE not in TRACES:
			TRACES[GLOBAL_FILE] = []

		TRACES[file].append(event)
		TRACES[GLOBAL_FILE].append(event)

		print(f"{event=} | {len(TRACES[file])} {len(TRACES[GLOBAL_FILE])}")

		dump_events(file, TRACES[file])
		dump_events(GLOBAL_FILE, TRACES[GLOBAL_FILE])


def create_begin(pid, thread_id, thread_name, time, name):
	return {"name": name, "ph": "B", "ts": time, "pid": pid, "tid": thread_id}

def create_end(pid, thread_id, thread_name, time, name):
	return {"name": name, "ph": "E", "ts": time, "pid": pid, "tid": thread_id}

def create_sync(time):
	return { "name": "clock_sync", "ph": "c", "ts": time, "args": { "sync_id": "guid1" } }

def create_complete(pid, thread_id, time, duration, name):
	return {"name": name, "ph": "X", "ts": time, "dur": duration, "pid": pid, "tid": thread_id }

def create_instant(pid, thread_id, time, instant_type, name):
	return {"name": name, "ph": "i", "ts": time, "pid": pid, "tid": thread_id, "s": instant_type}

def create_metadata(pid, thread_id, metadata_type, metadata_value):
	return {"name": metadata_type, "ph": "M", "pid": pid, "tid": thread_id, "args": { "name" : metadata_value } }

def create_trace(events):
	# return { "traceEvents": events, "displayTimeUnit": "us"} # Perfetto is ignoring displayTimeUnit according to their error pane.
	return { "traceEvents": events }

def dump_events(file_name, events):
	trace = create_trace(events)

	with open(f"files/{file_name}.json", 'w') as fp:
		json.dump(trace, fp)

if __name__ == '__main__':
	main()
