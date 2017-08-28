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
import java.net.URL
import java.net.URLConnection
import java.io.InputStreamReader
import java.io.BufferedReader
import java.net.HttpURLConnection

private fun getNowTime(): Long {
     return Date().getTime()
}
val jedis: Jedis = Jedis("cache", 6379);

private fun checkVersion(buf: ByteArray, offset: Int, len: Int): ByteArray {
	val packet: Result = Packer.decode(buf, offset, len)
	System.out.println("对方发送的消息：" + packet)
	val mac: ByteArray = packet.mac
	var str_mac: String = ""
	mac.map{
		str_mac = str_mac + String.format("%02X",it) + "-"
	}
	str_mac = str_mac.substring(0, str_mac.length - 1)
	println(str_mac)
	val timestamp = getNowTime()
	var sn: Int = 0
	if(packet.type == CommandType.DATA){
		try{
			val up_data: Data = packet.data
			sn = up_data.sn
			System.out.println(up_data.systemBoard)
			System.out.println(up_data.lockBoard)
			System.out.println(up_data.supervisorVersion)
			System.out.println(up_data.boxosVersion)
			System.out.println(up_data.version)
			val tests: Boolean = jedis.sismember("testset", str_mac);
			println(tests)
			/* 查看是否在白名单中 */
			if(tests == false){
				val supervisor_url = jedis.hget("upgrade.release.supervisor", up_data.systemBoard.toString()  + "-" + up_data.lockBoard.toString() + "-" + up_data.version.toString())
				if(supervisor_url != null){
					var url = supervisor_url
					val lastIndexOfDot: Int = url.lastIndexOf("/")
					val fileNameLength: Int = url.length
					val extension: String = url.substring(lastIndexOfDot+1, fileNameLength-4)
					val arr = extension.split("-")
					val systemBoard = arr[0].toInt()
					val lockBoard = arr[1].toInt()
					val version = arr[2].toInt()
					val supervisorChecksum = arr[3].toLong()
					// if(up_data.supervisorVersion < version) {
					var payload = Upgrade()
					payload.sn = sn
					payload.boxosVersion = up_data.boxosVersion
					payload.systemBoard = systemBoard
					payload.boxosUrl = null
					payload.lockBoard = lockBoard 
					payload.supervisorChecksum  = supervisorChecksum
					payload.supervisorUrl  = url 
					payload.supervisorVersion = version
					payload.version = up_data.version
					payload.timestamp = timestamp
					val payload_byte: ByteArray = Packer.encode(mac, payload)
					return payload_byte
					// }
				}
				val boxos_url = jedis.hget("upgrade.release.boxos", up_data.systemBoard.toString()  + "-" + up_data.lockBoard.toString() + "-" + up_data.version.toString())
					if(boxos_url != null) {
						var url = boxos_url 
						val lastIndexOfDot: Int = url.lastIndexOf("/")
						val fileNameLength: Int = url.length
						val extension: String = url.substring(lastIndexOfDot+1, fileNameLength-4)
						val arr = extension.split("-")
						val systemBoard = arr[0].toInt()
						val lockBoard = arr[1].toInt()
						val version = arr[2].toInt()
						val boxosChecksum = arr[3].toLong()
						// if(up_data.boxosVersion < version) {
						var payload = Upgrade()
						payload.sn = sn
						payload.boxosChecksum = boxosChecksum
						payload.boxosVersion = version
						payload.systemBoard = systemBoard 
						payload.boxosUrl = url
						payload.lockBoard = lockBoard 
						payload.supervisorUrl  = null 
						payload.supervisorVersion = up_data.supervisorVersion
						payload.version = up_data.version
						payload.timestamp = timestamp
						val payload_byte: ByteArray = Packer.encode(mac, payload)
						return payload_byte
					// }
				}
			} else{
				val supervisor_test = jedis.hget("upgrade.prerelease.supervisor", up_data.systemBoard.toString()  + "-" + up_data.lockBoard.toString() + "-" + up_data.version.toString())
				if(supervisor_test != null){
						var url = supervisor_test;
						val lastIndexOfDot: Int = url.lastIndexOf("/")
						val fileNameLength: Int = url.length
						val extension: String = url.substring(lastIndexOfDot+1, fileNameLength-4)
						val arr = extension.split("-")
						val systemBoard = arr[0].toInt()
						val lockBoard = arr[1].toInt()
						val version = arr[2].toInt()
						val supervisorChecksum = arr[3].toLong()
						// if(version > up_data.supervisorVersion){
						var payload = Upgrade()
						payload.sn = sn
						payload.boxosVersion = up_data.boxosVersion
						payload.systemBoard = systemBoard 
						payload.boxosUrl = null
						payload.lockBoard = lockBoard 
						payload.supervisorChecksum  = supervisorChecksum
						payload.supervisorUrl  = url 
						payload.supervisorVersion = version 
						payload.version = up_data.version
						payload.timestamp = timestamp
						val payload_byte: ByteArray = Packer.encode(mac, payload)
						return payload_byte
						// }
					}
					val boxos_test = jedis.hget("upgrade.prerelease.boxos", up_data.systemBoard.toString()  + "-" + up_data.lockBoard.toString() + "-" + up_data.version.toString())
					if(boxos_test!= null){
					var url_test = boxos_test;
					val lastIndexOfDot_test: Int = url_test.lastIndexOf("/")
					val fileNameLength_test: Int = url_test.length
					val extension_test: String = url_test.substring(lastIndexOfDot_test + 1, fileNameLength_test - 4)
					val arr_test = extension_test.split("-")
					val systemBoard_test = arr_test[0].toInt()
					val lockBoard_test = arr_test[1].toInt()
					val version_test = arr_test[2].toInt()
					val boxosChecksum = arr_test[3].toLong()
					// if(version_test > up_data.boxosVersion){
					var payload_test = Upgrade()
					payload_test.sn = sn
					payload_test.boxosChecksum = boxosChecksum
					payload_test.boxosVersion = version_test
					payload_test.systemBoard = systemBoard_test 
					payload_test.boxosUrl = url_test
					payload_test.lockBoard = lockBoard_test 
					payload_test.supervisorUrl  = null 
					payload_test.supervisorVersion = up_data.supervisorVersion 
					payload_test.version = up_data.version
					payload_test.timestamp = timestamp
					val payload_byte: ByteArray = Packer.encode(mac, payload_test)
					return payload_byte
						// }
					}
				}
			}catch(e:Exception){
				println(e)
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
	}else if(packet.type == CommandType.REGISTER) {
		val up_data: Register = packet.register
		val pin: Int = up_data.pin
		try{
			// println(String(mac))
			val url: URL = URL("http://59.110.16.108:8888/devices/" + str_mac)
			val urlConnection: URLConnection = url.openConnection()                                                // 打开连接
			val httpURLConnection: HttpURLConnection = urlConnection as HttpURLConnection;
			httpURLConnection.setRequestProperty("token", "boxservice")
			val br: BufferedReader =  BufferedReader(InputStreamReader(httpURLConnection.getInputStream(),"utf-8")); // 获取输入流
			val res = br.readText().toString()
			val register_result: JSONObject = JSONObject(res)
			httpURLConnection.disconnect()
      br.close()
			var payload = HardwareTable()
			payload.antenna = register_result.getInt("antenna") 
			payload.cardReader = register_result.getInt("card-reader") 
			payload.lockAmount = register_result.getInt("lock-amount")
			payload.routerBoard = register_result.getInt("router-board")
			payload.simNo = register_result.getInt("sim-no")
			// payload.speaker = register_result.getInt("speaker")
			payload.systemBoard = register_result.getInt("system-board") 
			payload.lockBoard = register_result.getInt("lock-board") 
			payload.sn = up_data.sn
			payload.timestamp = timestamp 
			payload.version = up_data.version 
			payload.wireless = register_result.getInt("wireless") 
			val payload_byte: ByteArray = Packer.encode(mac, payload)
			return payload_byte
		}catch(e:Exception){
			println(e)
			val register_json = jedis.get("pin." + pin.toString())
			if(register_json == null){
				jedis.set("pin." + pin.toString(),str_mac)
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
	val port: Int = 8080
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
} 
