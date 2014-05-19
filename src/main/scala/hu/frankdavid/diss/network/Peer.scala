package hu.frankdavid.diss.network

import com.esotericsoftware.kryonet.{Connection, Client}

/**
 * Represents a machine on the network
 */
class Peer(val connection: Either[Connection, Client], var capacity: Int = 0)