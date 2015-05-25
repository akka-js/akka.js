'use strict';

const SERVERS = 3
const SVG = d3.select("body").append("svg").attr("width", 400).attr("height", 400) 
const BASE_CIRCLE = { r: 150, x: 200, y: 200 }

var TEXT = SVG.append('text').attr('x', 110).attr('y', 60).attr('font-size', '30px')

function setText(str) {
  TEXT.text(str.length === 1 ? '0' + str : str);
}

const COLORS = {
  "follower" : "#8da0cb",
  "leader"   : "green",
  "candidate": "#8da0cb"
}

var LEADER = 0

function setupCircles(n) {
  var ret = []
  ret.push({ id: 0, r: 40, x: 200, y: 50 })

  var angle = (2 * Math.PI) / n
  var h = 2 * (BASE_CIRCLE.r * Math.sin(angle / 2))

  var otherAngle = Math.PI - ((Math.PI - angle) + Math.PI / 2)
  otherAngle += (Math.PI - angle) / 2

  var c1 = h * Math.sin(otherAngle)
  var c2 = h * Math.cos(otherAngle) 
  
  for(let i = 1; i < n; i++) {  
    ret.push({ id: i, r: 40, x: 200 + (i > n / 2 ? -c2 : c2), y: 50 + c1 })
  }
  return ret
}


function drawCircle(to, c) {
  var circle = to.append("circle").attr("cx", c.x).attr("cy", c.y).attr("r", c.r).style("fill", c.color || COLORS[c.state])
  if(typeof c.id !== "undefined") circle = circle.attr("id", 'member' + c.id)
  return circle
}


function drawServers() {
  var angle = 360 / SERVERS
  for(let i = 1; i <= SERVERS; i++) {
    SVG.append("circle").attr("cx", BASE_CIRCLE.x - 180 * Math.sin(angle / 2)).attr("cy", BASE_CIRCLE.y - 180 * Math.cos(angle / 2)).attr("r", 25).style("fill", "purple");
  }
}

function transition(color, from, to, cbl) {
  drawCircle(SVG, { r: 20, x: from.x, y: from.y, color: color }).transition().attr("cx", to.x).attr("cy", to.y).duration(2000).each("end", cbl || function(){}).remove()
}

/*
function transitionHeart(from, to) {
 var g = SVG.append('g').attr('transform', 'translate(' + (from.x - 15 ) + ',' + (from.y - 10) + ') scale(0.05)')
 g.append("path").attr("d", "M 297.29747,550.86823 C 283.52243,535.43191 249.1268,505.33855 220.86277,483.99412 C 137.11867,420.75228 125.72108,411.5999 91.719238,380.29088 C 29.03471,322.57071 2.413622,264.58086 2.5048478,185.95124 C 2.5493594,147.56739 5.1656152,132.77929 15.914734,110.15398 C 34.151433,71.768267 61.014996,43.244667 95.360052,25.799457 C 119.68545,13.443675 131.6827,7.9542046 172.30448,7.7296236 C 214.79777,7.4947896 223.74311,12.449347 248.73919,26.181459 C 279.1637,42.895777 310.47909,78.617167 316.95242,103.99205 L 320.95052,119.66445 L 330.81015,98.079942 C 386.52632,-23.892986 564.40851,-22.06811 626.31244,101.11153 C 645.95011,140.18758 648.10608,223.6247 630.69256,270.6244 C 607.97729,331.93377 565.31255,378.67493 466.68622,450.30098 C 402.0054,497.27462 328.80148,568.34684 323.70555,578.32901 C 317.79007,589.91654 323.42339,580.14491 297.29747,550.86823 z").style("fill", "#ff0000")
 g.transition().attr('transform', 'translate(' + (to.x - 15) + ',' + (to.y - 10) + ') scale(0.05)').duration(2000).remove()
}*/

var seen = 0
function transitionHeartBeat(from) {
  seen += 1
  if(!(seen % 6)) {
    seen = 0
    var from = circles.filter(function(e) { return e.id === from }).pop()
    if(from.id === LEADER) {
      circles.filter(function(e) { return e.id !== from }).forEach(function(to) {
        transition("red", from, to, function() {
          transition("blue", to, from)
        });
      })
    } 
  }
}

var circles = setupCircles(3).map(function(e) { e.state = "follower"; return e })
circles.map(function(e) { return drawCircle(SVG, e) })

function setState(id, what) {
  if(what === 'leader') LEADER = id
  d3.select('#member' + id).style("fill", COLORS[what])   
}
