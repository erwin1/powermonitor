var ratioChart;
var gridGauge;
var evGauge;
var pvGauge;
var monthPeakGauge;
var currentAverageGauge;
var newAverageEstimateGauge;



function refreshData() {
        $.get( "/metrics/live", function( d ) {

        monthPeakGauge.refresh(d.monthlyPowerPeakW);
        currentAverageGauge.refresh(d.importAverageW);
        newAverageEstimateGauge.refresh(d.peakEstimateW);
        if (d.importW > 0) {
             gridGauge.refresh(d.importW);
        } else {
            gridGauge.refresh(-d.exportW);
        }

        {
        let tsString = d.timestamp;
        let tIndex = tsString.indexOf("T");
        let plusIndex = tsString.indexOf("+");
        let dts = $.datepicker.parseDate( "yy-mm-dd", tsString.substring(0, tIndex) );
        $("#livedatatimestamp").html($.datepicker.formatDate("dd/mm/yy", dts) + " " + tsString.substring(tIndex + 1, plusIndex));
        }

        {
        let tsString = d.monthlyPowerPeakTimestamp;
        let tIndex = tsString.indexOf("T");
        let dotIndex = tsString.indexOf("+");
        let dts = $.datepicker.parseDate( "yy-mm-dd", tsString.substring(0, tIndex) );
        $("#monthPeakDate").html($.datepicker.formatDate("dd/mm/yy", dts) + " " + tsString.substring(tIndex + 1, dotIndex));
        }
    });
}

$(function () {
    gridGauge = new JustGage({
        id: "gridGauge", // the id of the html element
        value: 0,
        min: -4000,
        max: 4000,
        symbol: ' W',
        label: "",
        decimals: 0,
        gaugeWidthScale: 0.6,
        differential: true,
        labelMinFontSize: 14,
        minLabelMinFontSize: 14,
        maxLabelMinFontSize: 14,
        pointer: true,
        pointerOptions: {
          toplength: -15,
          bottomlength: 50,
          bottomwidth: 10,
          color: '#8e8e93',
          stroke: '#ffffff',
          stroke_width: 1,
          stroke_linecap: 'round'
        }
      });

    currentAverageGauge = new JustGage({
        id: "currentAverageGauge",
        value: 0,
        min: 0,
        max: 6000,
        symbol: ' W',
        label: "",
        decimals: 0,
        gaugeWidthScale: 0.6,
        pointer: true,
        labelMinFontSize: 14,
        minLabelMinFontSize: 14,
        maxLabelMinFontSize: 14,
        levelColorsGradient: true,
        levelColors: ["#a5fc03", "#fcf403", "#fcce03", "#fc6f03"],
        pointerOptions: {
          toplength: -15,
          bottomlength: 50,
          bottomwidth: 10,
          color: '#8e8e93',
          stroke: '#ffffff',
          stroke_width: 1,
          stroke_linecap: 'round'
        }
      });

    monthPeakGauge = new JustGage({
        id: "monthPeakGauge",
        value: 0,
        min: 0,
        max: 6000,
        symbol: ' W',
        label: "",
        decimals: 0,
        gaugeWidthScale: 0.6,
        pointer: true,
        labelMinFontSize: 14,
        minLabelMinFontSize: 14,
        maxLabelMinFontSize: 14,
        levelColorsGradient: true,
        levelColors: ["#a5fc03", "#fcf403", "#fcce03", "#fc6f03"],
        pointerOptions: {
          toplength: -15,
          bottomlength: 50,
          bottomwidth: 10,
          color: '#8e8e93',
          stroke: '#ffffff',
          stroke_width: 1,
          stroke_linecap: 'round'
        }
      });

    newAverageEstimateGauge = new JustGage({
        id: "newAverageEstimateGauge",
        value: 0,
        min: 0,
        max: 6000,
        symbol: ' W',
        label: "",
        decimals: 0,
        gaugeWidthScale: 0.6,
        pointer: true,
        labelMinFontSize: 14,
        minLabelMinFontSize: 14,
        maxLabelMinFontSize: 14,
        levelColorsGradient: true,
        levelColors: ["#a5fc03", "#fcf403", "#fcce03", "#fc6f03"],
        pointerOptions: {
          toplength: -15,
          bottomlength: 50,
          bottomwidth: 10,
          color: '#8e8e93',
          stroke: '#ffffff',
          stroke_width: 1,
          stroke_linecap: 'round'
        }
      });


    refreshData();
    setInterval(refreshData, 15000);
});