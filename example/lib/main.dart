import 'dart:io';

import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:great_cam/great_cam.dart';
import 'package:great_cam/great_cam_method_channel.dart';
import 'package:permission_handler/permission_handler.dart';

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
                ? Image.file(File(_platformVersion!))
                : IconButton(
                    icon: Icon(Icons.camera),
                    onPressed: () async {
                      if (!await Permission.camera.isGranted) {
                        await Permission.camera.request();
                        return;
                      }

                      initPlatformState();
                    })),
      ),
    );
  }
}
