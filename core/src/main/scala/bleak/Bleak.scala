package bleak

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{Channel, ChannelHandler, ChannelInitializer, ChannelOption}
import io.netty.handler.codec.http.{HttpObjectAggregator, HttpServerCodec}
import io.netty.handler.logging.LoggingHandler

class Bleak extends Application {

  protected val bossGroup = new NioEventLoopGroup(1)

  protected val workerGroup = new NioEventLoopGroup

  private var channel: Channel = _

  val Backlog: Int = 1024

  val MaxContentLength: Int = Short.MaxValue

  @volatile private var _host: String = "127.0.0.1"

  @volatile private var _port: Int = 7865

  def host: String = _host

  def port: Int = _port

  def run(host: String = this.host, port: Int = this.port): Unit =
    synchronized {
      _host = host
      _port = port
      start()
      channel.closeFuture().sync()
    }

  def start(host: String = this.host, port: Int = this.port): Unit =
    synchronized {
      if (channel == null) {
        val bootstrap = new ServerBootstrap()
        bootstrap.option[Integer](ChannelOption.SO_BACKLOG, Backlog)
        bootstrap
          .group(bossGroup, workerGroup)
          .channel(classOf[NioServerSocketChannel])
          .handler(new LoggingHandler())
          .childHandler(createInitializer())
        channel = bootstrap.bind(host, port).sync().channel()
      }
    }

  def createInitializer(): ChannelHandler = new NettyInitializer(this, MaxContentLength)
}

object Bleak {
  def apply(): Bleak = new Bleak
}

class NettyInitializer(app: Application, maxContentLength: Int)
    extends ChannelInitializer[SocketChannel] {
  override def initChannel(ch: SocketChannel): Unit =
    ch.pipeline()
      .addLast(new HttpServerCodec())
      .addLast(new HttpObjectAggregator(Int.MaxValue))
      .addLast(new RoutingHandler(app))
}
