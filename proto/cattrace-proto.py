import sys
import json
import perfetto_trace_pb2 as Perfetto

# https://android.googlesource.com/platform/external/perfetto/+/refs/heads/main/protos/
# https://android.googlesource.com/platform/external/perfetto/+/refs/heads/main/protos/perfetto/trace/perfetto_trace.proto
# protoc -I=. --python_out=. ./perfetto_trace.proto

# https://perfetto.dev/docs/reference/trace-packet-proto
# https://perfetto.dev/docs/reference/synthetic-track-event

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

def write(root, path):
	with open(f"../files/{path}.perfetto_trace", "wb") as fd:
		fd.write(root.SerializeToString())

def read(path):
	r = Perfetto.Trace()
	with open(f"../files/{path}.perfetto_trace", "rb") as fd:
		r.ParseFromString(fd.read())
	return r

def new_process(uuid, pid, name):
	event = Perfetto.TrackDescriptor()
	event.uuid = uuid
	process = Perfetto.ProcessDescriptor()
	process.pid = pid
	process.process_name = name
	event.process.CopyFrom(process)

	packet = Perfetto.TracePacket()
	packet.track_descriptor.CopyFrom(event)

	return packet

def new_thread(uuid, parent_uuid, pid, tid, name):
	event = Perfetto.TrackDescriptor()
	event.uuid = uuid
	event.parent_uuid = parent_uuid
	thread = Perfetto.ThreadDescriptor()
	thread.pid = pid
	thread.tid = tid
	thread.thread_name = name
	event.thread.CopyFrom(thread)

	packet = Perfetto.TracePacket()
	packet.track_descriptor.CopyFrom(event)

	return packet

def new_event(track_uuid, event_type, timestamp, name = None, flow_ids = None):
	packet = Perfetto.TracePacket()
	packet.timestamp = timestamp
	packet.trusted_packet_sequence_id = TRUSTED_PACKET_SEQUENCE_ID

	track_event = Perfetto.TrackEvent()
	track_event.type = event_type
	track_event.track_uuid = track_uuid
	if name:
		track_event.name = name
	if flow_ids:
		track_event.flow_ids.extend(flow_ids)

	packet.track_event.CopyFrom(track_event)
	return packet

def legacy_complete_event(track_uuid, timestamp, duration, name = None, flow_ids = None):
	start_time = timestamp
	end_time = start_time + duration

	event = [
	new_event(track_uuid, Perfetto.TrackEvent.Type.TYPE_SLICE_BEGIN, start_time, name, flow_ids = flow_ids),
	new_event(track_uuid, Perfetto.TrackEvent.Type.TYPE_SLICE_END, end_time, name, flow_ids = flow_ids)
	]

	return event


TRUSTED_PACKET_SEQUENCE_ID = 3903809   # Generate *once*, use throughout. Can we just the same everytime we convert a trace?

# Replace with a more reliable method?
CURRENT_UUID = 0

def next_uuid():
	global CURRENT_UUID
	CURRENT_UUID = CURRENT_UUID + 1
	return CURRENT_UUID

def field(name, e):
	return e[name] if name in e else None

def convert(data, destination_file):
	print(f"{destination_file=}")
	root = Perfetto.Trace()
	pids = {}
	tids = {}

	first_timestamp = -1 # Disabled

	# Observations:
	# "ts" can't start a 0, Perfetto will add 1 and use it as the first valid value.

	events = []
	for e in data["traceEvents"]:
		print(f"{e=}")
		
		event_type = field("ph", e)
		pid = field("pid", e) % (2**31-1) # Constrain the value so it does not go over i32. (Might happen if someone uses the current timestamp as the PID)
		tid = field("tid", e)
		timestamp = field("ts", e)
		name = field("name", e)
		flow_ids = field("flow_ids", e)
		args = field("args", e)
		duration = field("dur", e)

		if first_timestamp == None:
			first_timestamp = timestamp
			print(f"{first_timestamp=}")

		timestamp = timestamp - first_timestamp # TODO override ClockSnapshot?

		if pid:
			if pid not in pids:
				uuid = next_uuid()
				pids[pid] = (uuid, pid, "")

		if tid:
			if tid not in tids:
				uuid = next_uuid()
				(process_uuid, _, _) = pids[pid] if pid in pids else (next_uuid(), pid, "") # should already be there.

				tids[tid] = (uuid, process_uuid, pid, tid, "")
		
		(thread_uuid, _, _, _, _) = tids[tid]

		if event_type == "B":			
			events.append(new_event(thread_uuid, Perfetto.TrackEvent.Type.TYPE_SLICE_BEGIN, timestamp, name, flow_ids = flow_ids))

		elif event_type == "E":
			events.append(new_event(thread_uuid, Perfetto.TrackEvent.Type.TYPE_SLICE_END, timestamp, name, flow_ids = flow_ids))

		elif event_type == "X":
			events.extend(legacy_complete_event(thread_uuid, timestamp, duration, name, flow_ids = flow_ids))

		elif event_type == "i":
			events.append(new_event(thread_uuid, Perfetto.TrackEvent.Type.TYPE_INSTANT, timestamp, name, flow_ids = flow_ids))

		elif event_type == "M":
			new_value = args["name"] if "name" in args else ""

			if name == "thread_name":
				if tid not in tids:
					uuid = next_uuid()
					(process_uuid, _, _) = pids[pid] if pid in pids else (next_uuid(), pid, "") # should already be there.
					tids[tid] = (uuid, process_uuid, pid, tid, new_value)
				else:
					(uuid, process_uuid, pid, tid, current_name) = tids[tid]
					tids[tid] = (uuid, process_uuid, pid, tid, new_value)

			elif name == "process_name":
				if pid not in pids:
					uuid = next_uuid()
					pids[pid] = (uuid, pid, name)
				else:
					(uuid, pid, current_name) = pids[pid]
					pids[pid] = (uuid, pid, new_value)


			else: 
				print(f"Ignored | {name=}")
		else:
			print(f"Ignored | {event_type=}")
			continue

	metadata = []
	for pid in pids:
		(uuid, pid, name) = pids[pid]
		metadata.append(new_process(uuid, pid, name))

	for tid in tids:
		(uuid, process_uuid, pid, tid, name) = tids[tid]
		metadata.append(new_thread(uuid, process_uuid, pid, tid, name))

	root.packet.extend(metadata)
	root.packet.extend(events)
	write(root, destination_file)
	r = read(destination_file)
	print(f"{r=}")

def convert_file(file_path):
	with open(f"{file_path}.json", 'r') as f:
		data = json.load(f)
		convert(data, file_path)

def main():
	# Replicates the example in https://perfetto.dev/docs/reference/synthetic-track-event

	# This is is incomplete but shows how you would convert the events to a protobuf binary file.
	# There's also # https://androidx.tech/artifacts/benchmark/benchmark-common/1.1.0-beta06-source/androidx/benchmark/UserspaceTracing.kt.html 
	# which would allow you to write the file directly on the device.

	root = Perfetto.Trace()
	print(f"{root=}")

	process_uuid = 894893984
	pid = 1234
	new_process_packet = new_process(process_uuid, pid, "Name of Process")	
	thread_uuid = 49083589894
	new_thread_packet = new_thread(thread_uuid, process_uuid, pid, 5678, "Main Thread")
	
	root.packet.extend([
		new_process_packet,
	 	new_thread_packet,
	 	new_event(thread_uuid, Perfetto.TrackEvent.Type.TYPE_SLICE_BEGIN, 200, "My special parent"),
	 	new_event(thread_uuid, Perfetto.TrackEvent.Type.TYPE_SLICE_BEGIN, 250, "My special child"),
	 	new_event(thread_uuid, Perfetto.TrackEvent.Type.TYPE_INSTANT, 285, ""),
	 	new_event(thread_uuid, Perfetto.TrackEvent.Type.TYPE_SLICE_END, 290, ""),
	 	new_event(thread_uuid, Perfetto.TrackEvent.Type.TYPE_SLICE_END, 300, ""),
	 	])

	write(root, "example_1")
	r = read("example_1")
	print(f"{r=}")

	convert_file("../test_files/test_a")
	convert_file("../test_files/test_b")
	convert_file("../test_files/test_c")
	convert_file("../test_files/test_d")
	convert_file("../test_files/test_1")
	convert_file("../test_files/test_2")

if __name__ == '__main__':
	main()
