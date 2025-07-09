import 'dart:developer';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:telpo_flutter_sdk/src/models/telpo_printer_configuration.dart';
import 'package:telpo_flutter_sdk/telpo_flutter_sdk.dart';

class TelpoFlutterChannel {
  TelpoFlutterChannel._() {
    _platform = MethodChannel(_channelName);
  }

  static TelpoFlutterChannel get instance => TelpoFlutterChannel._();

  late MethodChannel _platform;

  final String _channelName = 'me.aljan.telpo_flutter_sdk/telpo';

  /// Returns an [Enum] of type [TelpoStatus] indicating current status of
  /// underlying Telpo Device.
  Future<TelpoStatus> checkStatus() async {
    try {
      final status = await _platform.invokeMethod('checkStatus');

      switch (status) {
        case 'STATUS_OK':
          return TelpoStatus.ok;
        case 'STATUS_NO_PAPER':
          return TelpoStatus.noPaper;
        case 'STATUS_OVER_FLOW':
          return TelpoStatus.cacheIsFull;

        case 'STATUS_OVER_UNKNOWN':
        default:
          return TelpoStatus.unknown;
      }
    } catch (_) {
      return TelpoStatus.unknown;
    }
  }

  /// Connect with underlying Telpo device if any.
  ///
  /// Returns a [bool] whether connected successfully or not.
  Future<bool> connect() async {
    try {
      final connected = await _platform.invokeMethod('connect');

      return connected ?? false;
    } catch (e) {
      log('TELPO EXCEPTION: $e');

      return false;
    }
  }

  /// Disconnect from Telpo device.
  ///
  /// Returns a [bool] whether disconnected successfully or not.
  Future<bool> disconnect() async {
    try {
      final disconnected = await _platform.invokeMethod('disconnect');

      return disconnected ?? false;
    } catch (e) {
      log('TELPO EXCEPTION: $e');

      return false;
    }
  }

  /// Returns a nullable [bool] whether or not connected with Telpo device.
  Future<bool> isConnected() async {
    try {
      final isConnected = await _platform.invokeMethod('isConnected');

      return isConnected ?? false;
    } catch (e) {
      log('TELPO EXCEPTION: $e');

      return false;
    }
  }

  /// Takes [List<PrintData>] to be printed and returns [PrintResult] enum as
  /// an indicator for result of the process
  ///
  /// If [PrintResult.success] the data printed successfully, if else process
  /// blocked by some exception. See the result enum for more info.
  Future<PrintResult> print(TelpoPrintSheet data) async {
    return await _handlePrint(
      () async {
        await _platform.invokeMethod(
          'print',
          {
            "data": data.asJson,
          },
        );
      },
    );
  }

  /// Takes [List<PrintData>] to be printed and returns [PrintResult] enum as
  /// an indicator for result of the process
  ///
  /// If [PrintResult.success] the data printed successfully, if else process
  /// blocked by some exception. See the result enum for more info.
  Future<PrintResult> printWithJson(List<Map<String, dynamic>?> json) async {
    return await _handlePrint(
      () async {
        await _platform.invokeMethod(
          'print',
          {
            "data": json,
          },
        );
      },
    );
  }

  Future<PrintResult> _handlePrint(AsyncCallback printCallback) async {
    try {
      await printCallback();

      return PrintResult.success;
    } on PlatformException catch (e) {
      switch (e.code) {
        case '3':
          return PrintResult.noPaper;
        case '4':
          return PrintResult.lowBattery;
        case '12':
          return PrintResult.overHeat;
        case '13':
          return PrintResult.dataCanNotBeTransmitted;
        default:
          log('TELPO EXCEPTION: $e, code: ${e.code}');

          return PrintResult.other;
      }
    }
  }

  /// [TelpoPrinterConfiguration] configuration
  ///
  ///
  Future<bool> configurePrinter(Map<String, dynamic> configuration) async {
    try {
      final isSuccess = await _platform.invokeMethod(
        'configurePrinter',
        {
          "data": configuration,
        },
      );

      return isSuccess ?? false;
    } catch (e) {
      log('TELPO EXCEPTION: $e');

      return false;
    }
  }
}
