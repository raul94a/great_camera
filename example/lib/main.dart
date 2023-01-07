import 'dart:io';

import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:great_cam/great_cam.dart';
import 'package:great_cam/great_cam_method_channel.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:video_player/video_player.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String? _platformVersion;
  final _methodChannel = GreatCam();

  @override
  void initState() {
    super.initState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String? platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    // We also handle the message potentially returning null.
   
    
      print('LA IMAGEN ESTÁ EN: $platformVersion');
      
      print('LA IMAGEN ESTÁ EN: $platformVersion');
      platformVersion = (await _methodChannel.startCamera());

      print('LA IMAGEN ESTÁ EN: $platformVersion');
   
    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
   setState(() {

      
      _platformVersion = platformVersion;
    });
    // return platformVersion;
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
            child: _platformVersion != null
                ? VideoPlayerScreen(file: _platformVersion!)
                : IconButton(
                    icon: Icon(Icons.camera),
                    onPressed: () async {
                    
                      bool shouldBlock = false;
                      if (!await Permission.camera.isGranted) {
                        await Permission.camera.request();
                        return;
                      }
                      if(!await Permission.microphone.isGranted){
                        await Permission.microphone.request();
                        return;
                      }
                      if(!await Permission.storage.isGranted){
                        await Permission.storage.request();
                        return;
                      }
                      if(!await Permission.manageExternalStorage.isGranted){
                      await Permission.manageExternalStorage.request();
                        return;
                      }
                    

                      initPlatformState();
                    })),
      ),
    );
  }
}


class VideoPlayerScreen extends StatefulWidget {
  const VideoPlayerScreen({super.key, required this.file});

  final String file;

  @override
  State<VideoPlayerScreen> createState() => _VideoPlayerScreenState();
}

class _VideoPlayerScreenState extends State<VideoPlayerScreen> {
  late VideoPlayerController _controller;
  late Future<void> _initializeVideoPlayerFuture;

  @override
  void initState() {
    super.initState();

    // Create and store the VideoPlayerController. The VideoPlayerController
    // offers several different constructors to play videos from assets, files,
    // or the internet.
    _controller = VideoPlayerController.file(
    File(widget.file),

    );

    _initializeVideoPlayerFuture = _controller.initialize();
    _controller.play();
  }

  @override
  void dispose() {
    // Ensure disposing of the VideoPlayerController to free up resources.
    _controller.dispose();

    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // Complete the code in the next step.
    return FutureBuilder(
      future: _initializeVideoPlayerFuture,
      builder: (ctx,sn){

      if(sn.connectionState == ConnectionState.waiting){
          return CircularProgressIndicator();
      }
      else return VideoPlayer(_controller);
    });
  }
}