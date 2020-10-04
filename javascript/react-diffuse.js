var l = 256;
var A = Array(l*l);
var B = Array(l*l);
var running = true;
var actions = [];
var substep = 5;
var display;
var imgData;
var ctx;

async function boot(){
    display = document.getElementById("display");
    display.width = l;
    display.height = l;
    
    ctx = display.getContext('2d');
    imgData = ctx.createImageData(l, l);
    
    for(var i = 0; i<l*l; i++){
        A[i] = Math.random();
        B[i] = Math.random();
    }
    paint();
    var dA = Array(l*l);
    var dB = Array(l*l);
    var D_a = 0.5;
    var D_b = 0.2;
    var r_a =1.01;
    var r_b = -1.01;
    var r_ab = 1.01;
    var r_ba = -1.01;
    
    var dt = 0.01;
    var x;
    var y;
    
    var n = l*l;
    
    while(running){
        while(actions.length>0){
            let fun = actions.pop();
            fun();
        }
        for(var j = 0; j<substep; j++){
            for(var i = 0; i<n; i++){
                x = i%l;
                y = Math.floor(i/l);
            
                //here is the reaction diffusion equation.
                dA[i] = D_a*(dx2(A, x, y) + dy2(A, x, y)) + r_a*c(A, x, y) + r_ba*c(B, x, y);
                dB[i] = D_b*(dx2(B, x, y) + dy2(B, x, y)) + r_ab*c(A, x, y) + r_b*c(B, x, y);
            }
            
            for(var i = 0; i<n; i++){
                A[i] += dA[i]*dt;
                B[i] += dB[i]*dt;
            }
        }
        p = new Promise( function(resolve){ setTimeout(resolve, 1);} );
        await p;
        paint();
    }
}

function paint(){
    
    let data = imgData.data;
    
    for(var i = 0; i<l*l; i++){
            let g = ( 255 * A[i] );
            g = g<0?0:g;
            g = g>255?255:g;
            
            let b = (255 *B[i]);
            b = b<0?0:b;
            b = b>255?255:b;
            
            let r = 0;
            if(A[i] > 1){ 
                r = (255*( (A[i] - 1) / 10.0 ) );
                g = g-r;
            }
            if(B[i] > 1){ 
                r = ( 255 * ( (B[i] - 1) / 10.0 ) );
                b = b-r;
            }

            
            data[4*i] = r;
            data[4*i+1] = g;
            data[4*i+2] = b;
            data[4*i+3] = 255;
        }
    ctx.putImageData(imgData, 0, 0);
}

function c(arr, x, y){
    v = arr[x + y*l];
    if (isNaN(v)){
        console.log(x + ", " + y + " ::: NaN found");    
    }
        return v
}

function dx(  arr, x, y ){
        if(x==0){
          return 0.5*( c(arr, x+1, y) - c(arr, x, y) );
        } else if(x==(l-1)){
          return 0.5*( c(arr, x, y) - c(arr, x - 1, y) );
        } else{
          return 0.5*( c(arr, x + 1, y) - c(arr, x - 1, y) );
        }
    }
    
function dx2(  arr, x, y ){
    if(x==0){
        return ( c(arr, x+1, y) - c(arr, x, y) );
    } else if(x==(l-1)){
        return ( - c(arr, x, y) + c(arr, x - 1, y) );
    } else{
        return ( c(arr, x + 1, y) + c(arr, x - 1, y) - 2*c(arr, x, y ) );
    }
}

function dy( arr, x, y ){
    if(y==0){
        return 0.5*( c(arr, x, y+1) - c(arr, x, y) );
    } else if(y==(l-1)){
        return 0.5*( c(arr, x, y) - c(arr, x, y - 1) );
    } else{
        return 0.5*( c(arr, x, y + 1) - c(arr, x, y - 1) );
    }
}

function dy2( arr, x, y){
    if(y==0){
        return ( c(arr, x, y+1) - c(arr, x, y) );
    } else if(y==(l-1)){
        return ( -c(arr, x, y) + c(arr, x, y - 1) );
    } else{
        return ( c(arr, x, y + 1) + c(arr, x, y - 1) - 2*c(arr, x, y) );
    }
}
