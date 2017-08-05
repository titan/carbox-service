package com.fengchaohuzhu.box

import com.fengchaohuzhu.box.packet.Packer
import com.fengchaohuzhu.box.packet.Result
import com.fengchaohuzhu.box.packet.Data 
import com.fengchaohuzhu.box.packet.Upgrade
import com.fengchaohuzhu.box.packet.SyncTime
import com.fengchaohuzhu.box.packet.CommandType
import com.fengchaohuzhu.box.packet.HardwareTable
import com.fengchaohuzhu.box.packet.Register
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.ServerSocket 
import java.net.Socket 
import java.net.InetAddress.getByName
import java.util.Date
import java.nio.ByteBuffer
import java.util.TimeZone
import redis.clients.jedis.Jedis


private fun getNowTime(): Long {
     return Date().getTime()
}
val jedis: Jedis = Jedis("redis", 6379);

private fun checkVersion(buf: ByteArray, offset: Int, len: Int): ByteArray {
	val packet: Result = Packer.decode(buf, offset, len)
	System.out.println("对方发送的消息：" + packet)
	val mac: ByteArray = packet.mac
	val timestamp = getNowTime()
	var sn: Int = 0
	if(packet.type == CommandType.DATA){
		val up_data: Data = packet.data
		sn = up_data.sn
		System.out.println(up_data.androidBoard)
		System.out.println(up_data.lockBoard)
		val supervisor_json = jedis.hget("supervisor", up_data.androidBoard.toString()  + ":" + up_data.lockBoard.toString())
		System.out.println(supervisor_json)
		if(supervisor_json != null){
			val supervisor_result: JSONObject = JSONObject(supervisor_json)
			System.out.println(supervisor_result)
			if(up_data.supervisorVersion != supervisor_result.getInt("version")) {
					var payload = Upgrade()
					payload.sn = sn
					payload.boxosVersion = up_data.boxosVersion
					payload.androidBoard = supervisor_result.getInt("androidBoard")
					payload.boxosUrl = null
					payload.lockBoard = supervisor_result.getInt("lockBoard") 
					payload.supervisorUrl  = supervisor_result.getString("url")
					payload.supervisorVersion = supervisor_result.getInt("version") 
					payload.version = 1
					payload.timestamp = timestamp
					val payload_byte: ByteArray = Packer.encode(mac, payload)
					return payload_byte
			}
		}
		val boxos_json = jedis.hget("boxos", up_data.androidBoard.toString()  + ":" + up_data.lockBoard.toString())
		System.out.println(boxos_json)
		// if(boxos_json != null) {
		if(boxos_json == null) {
					println("11111111111")
			// val boxos_result: JSONObject = JSONObject(boxos_json)
			// val boxos_result: JSONObject = {version:2,androidBoard:2,url:"http://192.168.1.145:8888/boxos_v1.0.0_2017-08-01_fchb.apk",lockBoard:2} 
			// System.out.println(boxos_result)
			// if(up_data.boxosVersion != boxos_result.getInt("version")) {
					var payload = Upgrade()
					payload.sn = sn
					// payload.boxosVersion = boxos_result.getInt("version")
					// payload.androidBoard = boxos_result.getInt("androidBoard")
					// payload.boxosUrl = boxos_result.getString("url")
					// payload.lockBoard = boxos_result.getInt("lockBoard") 
					payload.boxosVersion = 2
					payload.androidBoard = 2
					payload.boxosUrl = "http://192.168.1.145:8888/boxos_v1.0.0_2017-08-01_fchb.apk"
					payload.lockBoard = 2
					payload.boxosChecksum  = 123456
					payload.supervisorChecksum  = 123456
					payload.supervisorUrl  = null
					payload.supervisorVersion = up_data.supervisorVersion 
					payload.version = 1
					payload.timestamp = timestamp
					val payload_byte: ByteArray = Packer.encode(mac, payload)
					return payload_byte
			// }
		}
	}else if(packet.type == CommandType.REGISTER) {
		val up_data: Register = packet.register
		val pin: Int = up_data.pin
		val register_json = jedis.hget("register", mac.toString()  + ":" + pin.toString())
		if(register_json != null){
			val register_result: JSONObject = JSONObject(register_json)
			var payload = HardwareTable()
			payload.androidBoard = register_result.getInt("andriodBoard") 
			payload.antenna = register_result.getInt("antenna") 
			payload.cardReader =register_result.getInt("cardReader") 
			payload.lockBoard = register_result.getInt("lockBoard") 
			payload.sn = register_result.getInt("sn") 
			payload.timestamp = register_result.getLong("timestamp") 
			payload.version = register_result.getInt("version") 
			payload.wireless = register_result.getInt("wireless") 
			val payload_byte: ByteArray = Packer.encode(mac, payload)
			return payload_byte
		}else{
			jedis.hset("wait-register", mac.toString(), pin.toString())
		}
	}
	val timeZone = TimeZone.getDefault();
	val zone = timeZone.getOffset(0) // 获取的是相差的毫秒　如8区值为8*60*60*1000 = 28800000
	var payload = SyncTime()
	payload.sn = sn
	payload.version = 0
	payload.timestamp = timestamp
	payload.zone = zone
	val payload_byte: ByteArray = Packer.encode(mac, payload)
	return payload_byte
}

public fun main(args: Array<String>){
	System.out.println("started? so amazing")
	val port: Int = 7000
	val ip = getByName("0.0.0.0")
	val getSocket = DatagramSocket(port, ip) 
	System.out.println("Server run")
	while(true){
		var buf: ByteArray = ByteArray(1024)
		val getPacket = DatagramPacket(buf, buf.size)
		getSocket.receive(getPacket)
		val packet: ByteArray = getPacket.getData()
		val offset: Int = getPacket.getOffset()
		val len: Int = getPacket.getLength()
		val new_payload = checkVersion(packet, offset, len)
		val sendIP = getPacket.getAddress()
		val sendPort: Int = getPacket.getPort()  
		val sendAddress = getPacket.getSocketAddress()
		val backBuf: ByteArray = new_payload
		val sendPacket = DatagramPacket(backBuf, backBuf.size, sendAddress)
		getSocket.send(sendPacket)
		System.out.println("end by" + sendIP + ":" + sendPort)
	}
	// getSocket.close()
} 
